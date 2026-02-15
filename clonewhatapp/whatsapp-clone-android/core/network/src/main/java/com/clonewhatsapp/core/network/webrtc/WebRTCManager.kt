package com.clonewhatsapp.core.network.webrtc

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Gestor de WebRTC puro. No depende de SignalR.
 * Maneja la creacion y ciclo de vida de PeerConnection, audio y video tracks.
 */
@Singleton
class WebRTCManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WebRTCManager"
        private const val LOCAL_AUDIO_TRACK_ID = "local_audio_track"
        private const val LOCAL_VIDEO_TRACK_ID = "local_video_track"
        private const val LOCAL_STREAM_ID = "local_stream"
        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720
        private const val VIDEO_FPS = 30
    }

    // Servidor STUN de Google (gratuito)
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
            .createIceServer()
    )

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var eglBase: EglBase? = null

    // Audio
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    // Video
    private var videoSource: VideoSource? = null
    private var localVideoTrackInternal: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var isFrontCamera: Boolean = true

    // -- StateFlows publicos --
    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private val _isAudioEnabled = MutableStateFlow(true)
    val isAudioEnabled: StateFlow<Boolean> = _isAudioEnabled.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()

    // Candidatos ICE para enviar al otro peer (serializados como JSON)
    private val _iceCandidates = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val iceCandidates: SharedFlow<String> = _iceCandidates.asSharedFlow()

    // Estado de la conexion ICE
    private val _iceConnectionState = MutableStateFlow(PeerConnection.IceConnectionState.NEW)
    val iceConnectionState: StateFlow<PeerConnection.IceConnectionState> = _iceConnectionState.asStateFlow()

    /**
     * Inicializa el PeerConnectionFactory.
     * Debe llamarse antes de crear ofertas o respuestas.
     */
    fun initialize() {
        Log.d(TAG, "Inicializando WebRTC")

        // Inicializar libreria WebRTC
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        eglBase = EglBase.create()

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase!!.eglBaseContext, true, true
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase!!.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()

        Log.i(TAG, "WebRTC inicializado correctamente")
    }

    /**
     * Retorna el contexto EGL para renderizar video.
     */
    fun getEglBaseContext(): EglBase.Context? = eglBase?.eglBaseContext

    /**
     * Crea la PeerConnection con el observer de eventos.
     */
    private fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            peerConnectionObserver
        )

        Log.d(TAG, "PeerConnection creada")
    }

    /**
     * Crea una oferta SDP para iniciar una llamada.
     * @return SDP offer como string.
     */
    suspend fun createOffer(): String {
        ensurePeerConnection()
        setupLocalMediaTracks()

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        val sdp = suspendCoroutine<SessionDescription> { continuation ->
            peerConnection?.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sessionDescription: SessionDescription) {
                    Log.d(TAG, "Oferta SDP creada exitosamente")
                    continuation.resume(sessionDescription)
                }

                override fun onCreateFailure(error: String?) {
                    Log.e(TAG, "Error al crear oferta SDP: $error")
                    continuation.resumeWithException(
                        RuntimeException("Error al crear oferta SDP: $error")
                    )
                }

                override fun onSetSuccess() {}
                override fun onSetFailure(error: String?) {}
            }, constraints)
        }

        // Establecer la descripcion local
        suspendCoroutine<Unit> { continuation ->
            peerConnection?.setLocalDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    Log.d(TAG, "Descripcion local establecida")
                    continuation.resume(Unit)
                }

                override fun onSetFailure(error: String?) {
                    Log.e(TAG, "Error al establecer descripcion local: $error")
                    continuation.resumeWithException(
                        RuntimeException("Error al establecer descripcion local: $error")
                    )
                }

                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onCreateFailure(p0: String?) {}
            }, sdp)
        }

        return sdp.description
    }

    /**
     * Crea una respuesta SDP a partir de una oferta remota.
     * @param remoteSdp La oferta SDP remota.
     * @return SDP answer como string.
     */
    suspend fun createAnswer(remoteSdp: String): String {
        ensurePeerConnection()
        setupLocalMediaTracks()

        // Establecer la descripcion remota (oferta)
        val remoteDescription = SessionDescription(SessionDescription.Type.OFFER, remoteSdp)
        suspendCoroutine<Unit> { continuation ->
            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    Log.d(TAG, "Descripcion remota establecida (oferta)")
                    continuation.resume(Unit)
                }

                override fun onSetFailure(error: String?) {
                    Log.e(TAG, "Error al establecer descripcion remota: $error")
                    continuation.resumeWithException(
                        RuntimeException("Error al establecer descripcion remota: $error")
                    )
                }

                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onCreateFailure(p0: String?) {}
            }, remoteDescription)
        }

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        // Crear respuesta
        val sdp = suspendCoroutine<SessionDescription> { continuation ->
            peerConnection?.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(sessionDescription: SessionDescription) {
                    Log.d(TAG, "Respuesta SDP creada exitosamente")
                    continuation.resume(sessionDescription)
                }

                override fun onCreateFailure(error: String?) {
                    Log.e(TAG, "Error al crear respuesta SDP: $error")
                    continuation.resumeWithException(
                        RuntimeException("Error al crear respuesta SDP: $error")
                    )
                }

                override fun onSetSuccess() {}
                override fun onSetFailure(error: String?) {}
            }, constraints)
        }

        // Establecer descripcion local (respuesta)
        suspendCoroutine<Unit> { continuation ->
            peerConnection?.setLocalDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    Log.d(TAG, "Descripcion local establecida (respuesta)")
                    continuation.resume(Unit)
                }

                override fun onSetFailure(error: String?) {
                    Log.e(TAG, "Error al establecer descripcion local: $error")
                    continuation.resumeWithException(
                        RuntimeException("Error al establecer descripcion local: $error")
                    )
                }

                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onCreateFailure(p0: String?) {}
            }, sdp)
        }

        return sdp.description
    }

    /**
     * Establece la respuesta SDP remota (cuando el receptor contesta).
     */
    suspend fun setRemoteAnswer(sdpAnswer: String) {
        val remoteDescription = SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer)
        suspendCoroutine<Unit> { continuation ->
            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    Log.d(TAG, "Respuesta remota establecida")
                    continuation.resume(Unit)
                }

                override fun onSetFailure(error: String?) {
                    Log.e(TAG, "Error al establecer respuesta remota: $error")
                    continuation.resumeWithException(
                        RuntimeException("Error al establecer respuesta remota: $error")
                    )
                }

                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onCreateFailure(p0: String?) {}
            }, remoteDescription)
        }
    }

    /**
     * Agrega un candidato ICE recibido del peer remoto.
     * @param candidateJson Candidato ICE serializado como JSON.
     */
    fun addIceCandidate(candidateJson: String) {
        try {
            val json = JSONObject(candidateJson)
            val candidate = IceCandidate(
                json.getString("sdpMid"),
                json.getInt("sdpMLineIndex"),
                json.getString("candidate")
            )
            peerConnection?.addIceCandidate(candidate)
            Log.d(TAG, "Candidato ICE agregado: sdpMid=${candidate.sdpMid}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al agregar candidato ICE", e)
        }
    }

    /**
     * Alterna el estado del audio (mute/unmute).
     * @return true si el audio queda habilitado.
     */
    fun toggleAudio(): Boolean {
        val enabled = !(_isAudioEnabled.value)
        localAudioTrack?.setEnabled(enabled)
        _isAudioEnabled.value = enabled
        Log.d(TAG, "Audio ${if (enabled) "habilitado" else "silenciado"}")
        return enabled
    }

    /**
     * Alterna el estado del video (activar/desactivar camara).
     * @return true si el video queda habilitado.
     */
    fun toggleVideo(): Boolean {
        val enabled = !(_isVideoEnabled.value)
        localVideoTrackInternal?.setEnabled(enabled)
        _isVideoEnabled.value = enabled
        Log.d(TAG, "Video ${if (enabled) "habilitado" else "deshabilitado"}")
        return enabled
    }

    /**
     * Cambia entre camara frontal y trasera.
     */
    fun switchCamera() {
        videoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFront: Boolean) {
                isFrontCamera = isFront
                Log.d(TAG, "Camara cambiada a: ${if (isFront) "frontal" else "trasera"}")
            }

            override fun onCameraSwitchError(error: String?) {
                Log.e(TAG, "Error al cambiar camara: $error")
            }
        })
    }

    /**
     * Inicia la captura de video local desde la camara.
     */
    fun startLocalVideo() {
        val factory = peerConnectionFactory ?: run {
            Log.e(TAG, "PeerConnectionFactory no inicializada")
            return
        }

        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        // Buscar camara frontal primero, luego trasera
        val cameraName = deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: deviceNames.firstOrNull { enumerator.isBackFacing(it) }

        if (cameraName == null) {
            Log.e(TAG, "No se encontro ninguna camara disponible")
            return
        }

        isFrontCamera = enumerator.isFrontFacing(cameraName)

        videoCapturer = enumerator.createCapturer(cameraName, null)

        surfaceTextureHelper = SurfaceTextureHelper.create(
            "CaptureThread",
            eglBase?.eglBaseContext
        )

        videoSource = factory.createVideoSource(videoCapturer!!.isScreencast)
        videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
        videoCapturer?.startCapture(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS)

        localVideoTrackInternal = factory.createVideoTrack(LOCAL_VIDEO_TRACK_ID, videoSource).apply {
            setEnabled(true)
        }
        _localVideoTrack.value = localVideoTrackInternal
        _isVideoEnabled.value = true

        Log.i(TAG, "Video local iniciado con camara: $cameraName")
    }

    /**
     * Detiene la captura de video local.
     */
    fun stopLocalVideo() {
        try {
            videoCapturer?.stopCapture()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error al detener captura de video", e)
        }
        videoCapturer?.dispose()
        videoCapturer = null

        videoSource?.dispose()
        videoSource = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        localVideoTrackInternal = null
        _localVideoTrack.value = null
        _isVideoEnabled.value = false

        Log.d(TAG, "Video local detenido")
    }

    /**
     * Libera todos los recursos de WebRTC.
     */
    fun dispose() {
        Log.d(TAG, "Liberando recursos de WebRTC")

        stopLocalVideo()

        localAudioTrack?.dispose()
        localAudioTrack = null

        audioSource?.dispose()
        audioSource = null

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null

        peerConnectionFactory?.dispose()
        peerConnectionFactory = null

        eglBase?.release()
        eglBase = null

        _remoteVideoTrack.value = null
        _isAudioEnabled.value = true
        _isVideoEnabled.value = true
        _iceConnectionState.value = PeerConnection.IceConnectionState.NEW

        Log.i(TAG, "Recursos de WebRTC liberados")
    }

    // -- Metodos privados --

    /**
     * Asegura que exista una PeerConnection activa.
     */
    private fun ensurePeerConnection() {
        if (peerConnection == null) {
            createPeerConnection()
        }
    }

    /**
     * Configura los tracks locales de audio y video y los agrega a la PeerConnection.
     */
    private fun setupLocalMediaTracks() {
        val factory = peerConnectionFactory ?: return
        val pc = peerConnection ?: return

        // Configurar audio si no existe
        if (localAudioTrack == null) {
            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("echoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("noiseSuppression", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("autoGainControl", "true"))
            }
            audioSource = factory.createAudioSource(audioConstraints)
            localAudioTrack = factory.createAudioTrack(LOCAL_AUDIO_TRACK_ID, audioSource).apply {
                setEnabled(true)
            }
            _isAudioEnabled.value = true
        }

        // Agregar tracks a la PeerConnection
        localAudioTrack?.let { track ->
            pc.addTrack(track, listOf(LOCAL_STREAM_ID))
        }
        localVideoTrackInternal?.let { track ->
            pc.addTrack(track, listOf(LOCAL_STREAM_ID))
        }
    }

    /**
     * Observer de eventos de la PeerConnection.
     */
    private val peerConnectionObserver = object : PeerConnection.Observer {

        override fun onIceCandidate(candidate: IceCandidate) {
            Log.d(TAG, "Candidato ICE generado: ${candidate.sdpMid}")
            val json = JSONObject().apply {
                put("sdpMid", candidate.sdpMid)
                put("sdpMLineIndex", candidate.sdpMLineIndex)
                put("candidate", candidate.sdp)
            }
            _iceCandidates.tryEmit(json.toString())
        }

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            Log.d(TAG, "Estado de conexion ICE: $state")
            _iceConnectionState.value = state
        }

        override fun onTrack(transceiver: RtpTransceiver) {
            val track = transceiver.receiver.track()
            if (track is VideoTrack) {
                Log.d(TAG, "Track de video remoto recibido")
                _remoteVideoTrack.value = track
            }
        }

        override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
            Log.d(TAG, "Estado de PeerConnection: $state")
        }

        override fun onSignalingChange(state: PeerConnection.SignalingState) {
            Log.d(TAG, "Estado de senalizacion: $state")
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
            Log.d(TAG, "ICE recibiendo: $receiving")
        }

        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
            Log.d(TAG, "Estado de recopilacion ICE: $state")
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
            Log.d(TAG, "Candidatos ICE eliminados")
        }

        override fun onAddStream(stream: MediaStream) {
            Log.d(TAG, "Stream remoto agregado: ${stream.id}")
            stream.videoTracks?.firstOrNull()?.let { videoTrack ->
                _remoteVideoTrack.value = videoTrack
            }
        }

        override fun onRemoveStream(stream: MediaStream) {
            Log.d(TAG, "Stream remoto eliminado: ${stream.id}")
            _remoteVideoTrack.value = null
        }

        override fun onDataChannel(dataChannel: DataChannel) {
            Log.d(TAG, "DataChannel recibido: ${dataChannel.label()}")
        }

        override fun onRenegotiationNeeded() {
            Log.d(TAG, "Renegociacion necesaria")
        }

        override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {
            Log.d(TAG, "Track agregado desde receptor")
        }
    }
}
