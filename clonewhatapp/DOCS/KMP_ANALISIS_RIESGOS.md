# Análisis de Riesgos y Viabilidad Técnica: KMP para WhatsApp Clone

**Fecha:** 2026-02-14
**Proyecto:** WhatsApp Clone Android con Kotlin Multiplatform
**Autor:** Análisis técnico exhaustivo

---

## 1. Executive Summary

### Veredicto Final: **VIABLE CON CONDICIONES**

**Semáforo de riesgos:**
- 🟢 **BAJO riesgo:** Domain layer, REST API, Database, UI Android
- 🟡 **MEDIO riesgo:** SignalR integration, Push notifications, Media handling
- 🔴 **ALTO riesgo:** WebRTC implementation, Real-time performance at scale

**Recomendación:** Proceder con KMP, pero ejecutar **POCs obligatorios** en:
1. SignalR client (Semana 1) - GO/NO-GO decision
2. WebRTC Android (Semana 15) - GO/NO-GO decision

Si algún POC falla: tener planes de contingencia listos.

---

## 2. Análisis Detallado de Componentes Críticos

### 2.1 SignalR Client en KMP

#### 2.1.1 El Problema

**Situación actual:**
- Microsoft NO provee cliente SignalR oficial para KMP
- Existe cliente Java (Android) - versión 8.0.0
- Existe cliente Swift (iOS) - biblioteca community
- Protocolo SignalR es complejo (handshake, negociación, heartbeat, reconexión)

**Opciones evaluadas:**

| Opción | Pros | Contras | Viabilidad |
|--------|------|---------|------------|
| **A) Wrapper expect/actual** | Control total, usa libs nativas | Duplicación de lógica, mantenimiento | 🟢 ALTA |
| **B) WebSocket + protocolo custom** | 100% compartido | Muy complejo, reinventar rueda | 🔴 BAJA |
| **C) Library community (kmp-signalr)** | Rápido si existe | No existe madura en 2026 | 🔴 BAJA |
| **D) Fallback a Long Polling** | Simple, funciona siempre | No real-time, más carga servidor | 🟡 MEDIA |

**Decisión recomendada:** Opción A (wrapper)

#### 2.1.2 POC SignalR - Checklist de Validación

**Objetivos del POC (Semana 1):**

```kotlin
// commonMain - Interface compartida
interface SignalRClient {
    suspend fun connect(url: String, token: String)
    suspend fun disconnect()
    fun on(event: String, handler: (String) -> Unit)
    suspend fun invoke(method: String, vararg args: Any)
    val connectionState: StateFlow<ConnectionState>
}

// Validar estos casos:
✅ 1. Conexión inicial con JWT en query string
✅ 2. Recibir evento "ReceiveMessage" con JSON
✅ 3. Enviar método "SendMessage" con parámetros
✅ 4. Reconexión automática tras pérdida de red
✅ 5. Múltiples event handlers (ReceiveMessage, UserTyping, MessageStatusUpdated)
✅ 6. Manejo de errores de conexión
✅ 7. Desconexión limpia
✅ 8. Join/leave groups via "JoinChat"/"LeaveChat"
```

**Implementación POC Android:**

```kotlin
// androidMain/kotlin/.../SignalRClient.android.kt
import com.microsoft.signalr.*
import io.reactivex.Single

actual class SignalRClient {
    private var hubConnection: HubConnection? = null
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState = _connectionState.asStateFlow()

    actual suspend fun connect(url: String, token: String) = suspendCancellableCoroutine { cont ->
        hubConnection = HubConnectionBuilder.create(url)
            .withAccessTokenProvider(Single.defer { Single.just(token) })
            .withAutomaticReconnect() // ⚠️ Validar que funciona
            .build()

        hubConnection?.start()?.subscribe(
            {
                _connectionState.value = ConnectionState.CONNECTED
                cont.resume(Unit)
            },
            { error ->
                _connectionState.value = ConnectionState.DISCONNECTED
                cont.resumeWithException(error)
            }
        )

        hubConnection?.onClosed { error ->
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    actual fun on(event: String, handler: (String) -> Unit) {
        hubConnection?.on(event, { data: String ->
            handler(data)
        }, String::class.java)
    }

    actual suspend fun invoke(method: String, vararg args: Any) = suspendCoroutine { cont ->
        hubConnection?.invoke(method, *args)?.subscribe(
            { cont.resume(Unit) },
            { error -> cont.resumeWithException(error) }
        )
    }
}
```

**Criterios de éxito del POC:**
- ✅ Conexión exitosa en <2 segundos
- ✅ 0 crashes al recibir 100 mensajes consecutivos
- ✅ Reconexión automática tras airplane mode
- ✅ Múltiples event handlers sin memory leaks
- ✅ JSON parsing correcto (usar kotlinx.serialization)

**Criterios de fracaso (triggers para NO-GO):**
- ❌ No se puede establecer conexión con JWT
- ❌ Eventos no se reciben o se pierden mensajes
- ❌ Reconexión no funciona automáticamente
- ❌ Memory leaks o crashes frecuentes
- ❌ Latencia >500ms para recibir eventos

#### 2.1.3 Plan de Contingencia si SignalR POC falla

**Plan B: Long Polling**

```kotlin
class PollingRealtimeRepository(
    private val client: HttpClient,
    private val database: Database
) : IRealtimeRepository {

    private var lastMessageTimestamp: Long = 0
    private val pollInterval = 2000L // 2 segundos

    override suspend fun startPolling() {
        while (isActive) {
            try {
                val newMessages = client.get("/api/messages/since/$lastMessageTimestamp")
                    .body<List<MessageDTO>>()

                newMessages.forEach { msg ->
                    database.insertMessage(msg.toModel())
                    _messagesFlow.emit(msg.toModel())
                }

                if (newMessages.isNotEmpty()) {
                    lastMessageTimestamp = newMessages.maxOf { it.fechaEnvio }
                }

                delay(pollInterval)
            } catch (e: Exception) {
                delay(pollInterval * 2) // Backoff
            }
        }
    }
}
```

**Implicaciones:**
- ❌ No real-time (delay de 2s)
- ❌ Mayor carga en servidor
- ❌ Mayor consumo de batería
- ✅ Más simple, menos riesgo
- ✅ Funciona siempre

**Decisión:** Usar solo si SignalR falla completamente

---

### 2.2 WebRTC en KMP

#### 2.2.1 El Problema

**Desafío:**
- WebRTC API es completamente diferente entre Android y iOS
- No existe abstracción KMP estándar
- Muy dependiente de platform (hardware, permisos, rendering)

**Realidad del código compartido:**
```
Total WebRTC code: ~2,200 LOC
├── Shared (interfaces, models, state): ~200 LOC (9%)
├── Android implementation: ~1,000 LOC (45%)
└── iOS implementation: ~1,000 LOC (45%)
```

**Esto es ACEPTABLE.** No todo debe ser compartido.

#### 2.2.2 Estrategia de Abstracción

**Shared (commonMain):**

```kotlin
// domain/call/CallManager.kt
interface CallManager {
    suspend fun initiate(targetUserId: String, callType: CallType): Result<Call>
    suspend fun answer(callId: String): Result<Unit>
    suspend fun reject(callId: String): Result<Unit>
    suspend fun hangup(): Result<Unit>

    fun toggleAudio(enabled: Boolean)
    fun toggleVideo(enabled: Boolean)
    fun switchCamera()

    val callState: StateFlow<CallState>
    val localVideoTrack: StateFlow<VideoTrack?>
    val remoteVideoTrack: StateFlow<VideoTrack?>
}

sealed class CallState {
    object Idle : CallState()
    data class Ringing(val caller: Usuario) : CallState()
    object Connecting : CallState()
    data class Active(val duration: Int) : CallState()
    data class Ended(val reason: String) : CallState()
}

sealed class CallType {
    object Audio : CallType()
    object Video : CallType()
}

// Shared models
data class IceCandidate(val sdp: String, val sdpMid: String, val sdpMLineIndex: Int)
data class SessionDescription(val type: SdpType, val sdp: String)
enum class SdpType { OFFER, ANSWER }
```

**Android (androidMain):**

```kotlin
actual class CallManagerImpl(
    private val context: Context,
    private val signalRClient: SignalRClient
) : CallManager {

    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null

    init {
        initializePeerConnectionFactory()
    }

    private fun initializePeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }

    override suspend fun initiate(targetUserId: String, callType: CallType): Result<Call> {
        return withContext(Dispatchers.IO) {
            try {
                createPeerConnection()
                addLocalTracks(callType)

                val offer = createOffer()
                signalRClient.invoke("CallUser", targetUserId, callType.name, offer.sdp)

                Result.success(Call(id = UUID.randomUUID().toString(), type = callType))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun createOffer(): SessionDescription = suspendCoroutine { cont ->
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                cont.resume(SessionDescription(SdpType.OFFER, sdp.description))
            }
            override fun onCreateFailure(error: String) {
                cont.resumeWithException(Exception(error))
            }
            // ... otros métodos
        }, constraints)
    }

    private fun addLocalTracks(callType: CallType) {
        // Audio siempre
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio1", audioSource)
        peerConnection?.addTrack(localAudioTrack, listOf("stream1"))

        // Video solo si es videollamada
        if (callType == CallType.Video) {
            val videoSource = peerConnectionFactory.createVideoSource(false)
            val videoCapturer = createCameraCapturer()
            videoCapturer.initialize(
                SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext),
                context,
                videoSource.capturerObserver
            )
            videoCapturer.startCapture(1280, 720, 30)

            localVideoTrack = peerConnectionFactory.createVideoTrack("video1", videoSource)
            peerConnection?.addTrack(localVideoTrack, listOf("stream1"))
        }
    }
}
```

#### 2.2.3 POC WebRTC - Checklist de Validación

**Objetivos del POC (Semana 15):**

```
✅ 1. Crear PeerConnection con STUN servers
✅ 2. Capturar video de cámara local
✅ 3. Mostrar preview local en SurfaceView
✅ 4. Generar SDP offer
✅ 5. Enviar offer via SignalR (CallUser)
✅ 6. Recibir SDP answer via SignalR (ReceiveCallAnswer)
✅ 7. Set remote description
✅ 8. Intercambiar ICE candidates
✅ 9. Establecer conexión peer-to-peer
✅ 10. Mostrar video remoto en SurfaceView
✅ 11. Toggle audio (mute/unmute)
✅ 12. Toggle video (camera on/off)
✅ 13. Switch camera (front/back)
✅ 14. Hangup limpio
✅ 15. Probar en red WiFi local
✅ 16. Probar en red 4G (diferentes carriers)
```

**Criterios de éxito del POC:**
- ✅ Conexión P2P en <5 segundos
- ✅ Video fluido (>20fps)
- ✅ Audio sin eco ni distorsión
- ✅ ICE candidates se intercambian correctamente
- ✅ Funciona en redes NAT (via STUN)
- ✅ Hangup no deja recursos bloqueados

**Criterios de fracaso (triggers para alternativas):**
- ❌ No se establece conexión P2P (ICE failed)
- ❌ Video/audio con lag >500ms
- ❌ Crashes frecuentes
- ❌ Eco o feedback de audio
- ❌ No funciona en redes 4G

#### 2.2.4 Plan de Contingencia si WebRTC POC falla

**Opción 1: Usar SDK third-party**

```kotlin
// Stream Video SDK
implementation("io.getstream:stream-video-android:1.0.0")

// Pros:
✅ Maneja toda la complejidad WebRTC
✅ UI components incluidos
✅ TURN servers incluidos (bypass NAT)
✅ Escalable a llamadas grupales
✅ Soporte oficial

// Contras:
❌ Costo: ~$99/mes por 1000 usuarios
❌ Menos control
❌ Dependencia de servicio third-party
```

**Opción 2: Simplificar a solo Audio**

```kotlin
// Implementar solo llamadas de voz (más simple)
// - Sin video streaming
// - Solo audio tracks
// - Menor complejidad de UI
// - Menor uso de batería

// Postponer videollamadas a v2.0
```

**Opción 3: Usar Agora.io**

```kotlin
implementation("io.agora.rtc:full-sdk:4.3.0")

// Pros:
✅ Más económico que Stream
✅ Alta calidad
✅ Buena documentación
✅ KMP wrapper posible

// Contras:
❌ ~$40/mes base
❌ Vendor lock-in
```

**Decisión:** Intentar WebRTC nativo primero. Si falla, evaluar Stream SDK vs Agora.

---

### 2.3 Offline-First con SQLDelight

#### 2.3.1 Estrategia

**Principio:** "App debe funcionar sin conexión, sincronizar al reconectar"

**Implementación:**

```kotlin
class MessageRepositoryImpl(
    private val api: MessageApi,
    private val database: WhatsAppDatabase,
    private val signalR: SignalRClient
) : IMessageRepository {

    override suspend fun sendMessage(mensaje: Mensaje): Result<Mensaje> {
        // 1. Guardar localmente PRIMERO (con estado Pending)
        val localMessage = mensaje.copy(
            id = UUID.randomUUID().toString(),
            estado = EstadoMensaje.Enviado,
            sync = SyncState.PENDING // Campo extra
        )
        database.insertMessage(localMessage)

        // 2. Intentar enviar al servidor
        return try {
            val response = api.sendMessage(localMessage)

            // 3. Actualizar con ID del servidor
            database.updateMessage(
                localMessage.copy(
                    id = response.id,
                    sync = SyncState.SYNCED
                )
            )

            // 4. Enviar via SignalR
            signalR.invoke("SendMessage", response)

            Result.success(response)
        } catch (e: Exception) {
            // Mensaje queda en DB con estado PENDING
            // Background worker intentará reenviar
            Result.failure(e)
        }
    }

    override fun observeMessages(chatId: String): Flow<List<Mensaje>> {
        // SIEMPRE retornar de DB (single source of truth)
        return database.messageQueries
            .selectMessagesByChat(chatId)
            .asFlow()
            .mapToList()
            .map { it.map { dbMsg -> dbMsg.toModel() } }
    }
}
```

**Sync Worker (Android):**

```kotlin
class MessageSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: IMessageRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pendingMessages = database.getPendingMessages()

        var successCount = 0
        var failCount = 0

        pendingMessages.forEach { mensaje ->
            try {
                repository.sendMessage(mensaje)
                successCount++
            } catch (e: Exception) {
                failCount++
                // Retry con exponential backoff
            }
        }

        return when {
            failCount == 0 -> Result.success()
            successCount > 0 -> Result.retry()
            else -> Result.failure()
        }
    }
}

// Programar worker
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

val syncRequest = PeriodicWorkRequestBuilder<MessageSyncWorker>(15, TimeUnit.MINUTES)
    .setConstraints(constraints)
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "message_sync",
    ExistingPeriodicWorkPolicy.KEEP,
    syncRequest
)
```

**Riesgos:**
- 🟡 Conflictos de sincronización (dos usuarios editan mismo mensaje)
- 🟡 Orden de mensajes inconsistente
- 🟢 Consumo de storage (mitigado con limpieza periódica)

**Mitigación:**
- Usar timestamps del servidor como fuente de verdad
- Implementar conflict resolution (last-write-wins)
- Limitar mensajes en DB a últimos 30 días

---

### 2.4 Push Notifications

#### 2.4.1 Integración FCM

**Complejidad:** 🟢 BAJA (well-established pattern)

**Implementación:**

```kotlin
// AndroidManifest.xml
<service
    android:name=".FCMService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>

// FCMService.kt
class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data

        when (data["type"]) {
            "new_message" -> handleNewMessage(data)
            "missed_call" -> handleMissedCall(data)
            "status_update" -> handleStatusUpdate(data)
        }
    }

    private fun handleNewMessage(data: Map<String, String>) {
        val chatId = data["chatId"] ?: return
        val senderName = data["senderName"] ?: return
        val content = data["content"] ?: return

        // Crear notificación
        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(senderName)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(createChatIntent(chatId))
            .addAction(createReplyAction(chatId))
            .build()

        NotificationManagerCompat.from(this).notify(chatId.hashCode(), notification)
    }

    private fun createReplyAction(chatId: String): NotificationCompat.Action {
        val replyLabel = getString(R.string.reply)
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()

        val intent = Intent(this, ReplyReceiver::class.java).apply {
            putExtra("chatId", chatId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_MUTABLE
        )

        return NotificationCompat.Action.Builder(
            R.drawable.ic_send, replyLabel, pendingIntent
        ).addRemoteInput(remoteInput).build()
    }

    override fun onNewToken(token: String) {
        // Enviar token al backend
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api: PushApi = get() // Koin
                api.registerToken(token)
            } catch (e: Exception) {
                // Retry later
            }
        }
    }
}
```

**Riesgos:**
- 🟢 Token no se registra (retry logic)
- 🟡 Notificaciones no llegan (FCM throttling)
- 🟢 Deep linking falla (testing exhaustivo)

---

### 2.5 Media Handling (expect/actual)

#### 2.5.1 Image Picker

**Android:**

```kotlin
// androidMain
actual class MediaPicker(private val context: Context) {

    actual suspend fun pickImage(): MediaFile? = suspendCoroutine { cont ->
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }

        // Usar ActivityResultLauncher (requiere Activity reference)
        // O usar ActivityResultContracts.PickVisualMedia (Android 11+)

        val launcher = (context as ComponentActivity).activityResultRegistry.register(
            "image_picker",
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            uri?.let {
                val bytes = context.contentResolver.openInputStream(it)?.readBytes()
                val mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
                cont.resume(MediaFile(bytes!!, mimeType, "image.jpg", bytes.size.toLong()))
            } ?: cont.resume(null)
        }

        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    actual suspend fun capturePhoto(): MediaFile? {
        // Usar CameraX
        val imageCapture = ImageCapture.Builder().build()

        return suspendCoroutine { cont ->
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bytes = image.planes[0].buffer.toByteArray()
                        cont.resume(MediaFile(bytes, "image/jpeg", "photo.jpg", bytes.size.toLong()))
                        image.close()
                    }
                    override fun onError(exception: ImageCaptureException) {
                        cont.resumeWithException(exception)
                    }
                }
            )
        }
    }
}
```

**Riesgos:**
- 🟡 Permisos denegados (UX para solicitar)
- 🟢 Imagen muy grande (redimensionar antes de subir)
- 🟢 OOM en dispositivos low-end (usar BitmapFactory.Options)

**Mitigación:**

```kotlin
fun compressImage(bytes: ByteArray, maxSizeMB: Int = 5): ByteArray {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

    val scaleFactor = calculateScaleFactor(options, maxSizeMB)
    options.inJustDecodeBounds = false
    options.inSampleSize = scaleFactor

    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
    return outputStream.toByteArray()
}
```

---

## 3. Performance y Escalabilidad

### 3.1 Métricas Target

| Métrica | Target | Crítico |
|---------|--------|---------|
| App startup (cold) | <2s | <3s |
| App startup (warm) | <500ms | <1s |
| Chat list scroll | 60fps | 30fps |
| Message rendering | <16ms | <33ms |
| Image load | <500ms | <1s |
| Send message latency | <200ms | <500ms |
| Memory usage | <150MB | <250MB |
| APK size | <30MB | <50MB |
| Battery drain | <5%/hour | <10%/hour |

### 3.2 Optimizaciones Clave

#### 3.2.1 Database Queries

```sql
-- Índices críticos
CREATE INDEX idx_mensaje_chat_fecha ON Mensaje(chatId, fechaEnvio DESC);
CREATE INDEX idx_mensaje_remitente ON Mensaje(remitenteId);
CREATE INDEX idx_chat_actualizacion ON Chat(ultimaActualizacion DESC);

-- Query optimizada para chat list
SELECT
    c.*,
    m.contenido as ultimoMensaje,
    m.fechaEnvio as ultimaActualizacion,
    COUNT(CASE WHEN em.estado != 'Leido' AND m.remitenteId != :userId THEN 1 END) as noLeidos
FROM Chat c
LEFT JOIN Mensaje m ON m.chatId = c.id
LEFT JOIN EstadoMensajeUsuario em ON em.mensajeId = m.id AND em.usuarioId = :userId
GROUP BY c.id
ORDER BY ultimaActualizacion DESC
LIMIT :limit OFFSET :offset;
```

#### 3.2.2 Image Caching (Coil)

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizePercent(0.25) // 25% de RAM
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve("image_cache"))
            .maxSizeBytes(100 * 1024 * 1024) // 100MB
            .build()
    }
    .respectCacheHeaders(false)
    .build()

// En Compose
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(mensaje.urlArchivo)
        .crossfade(true)
        .transformations(RoundedCornersTransformation(8.dp.toPx()))
        .build(),
    contentDescription = null,
    modifier = Modifier.size(200.dp)
)
```

#### 3.2.3 Lazy Loading de Mensajes

```kotlin
@Composable
fun ChatDetailScreen(chatId: String, viewModel: MessageViewModel) {
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = messages,
            key = { it.id }
        ) { mensaje ->
            MessageBubble(mensaje)
        }

        // Trigger pagination al llegar al top
        item {
            LaunchedEffect(Unit) {
                if (listState.firstVisibleItemIndex == messages.size - 5) {
                    viewModel.loadMoreMessages()
                }
            }
        }
    }
}

// ViewModel
class MessageViewModel(
    private val getMessagesUseCase: GetMessagesUseCase
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Mensaje>>(emptyList())
    val messages = _messages.asStateFlow()

    private var currentPage = 0
    private val pageSize = 50

    fun loadMoreMessages() {
        viewModelScope.launch {
            val newMessages = getMessagesUseCase(
                chatId = chatId,
                offset = currentPage * pageSize,
                limit = pageSize
            )
            _messages.value = _messages.value + newMessages
            currentPage++
        }
    }
}
```

---

## 4. Comparación con Alternativas

### 4.1 KMP vs Full Native (Android + iOS separados)

| Aspecto | KMP | Full Native |
|---------|-----|-------------|
| **Tiempo total desarrollo** | 8 meses | 10 meses |
| **Costo desarrollo** | $80k | $100k |
| **Código compartido** | 65% | 0% |
| **Performance** | 95% native | 100% native |
| **Mantenimiento (anual)** | $20k | $30k |
| **Bugs duplicados** | NO | SÍ |
| **Curva aprendizaje** | Media-Alta | Media |
| **Tooling** | Bueno | Excelente |
| **Hiring pool** | Pequeño | Grande |
| **Future-proof** | ✅ | ✅ |

**Veredicto:** KMP gana en costo/tiempo si planeas iOS en <1 año.

---

### 4.2 KMP vs Flutter

| Aspecto | KMP | Flutter |
|---------|-----|---------|
| **Tiempo desarrollo** | 5.5 meses | 4 meses |
| **Código compartido** | 65% | 95% |
| **Native feeling** | 100% | 85% |
| **Performance** | Mejor | Buena |
| **Acceso APIs nativas** | Directo | Via channels |
| **WebRTC** | Nativo | Plugins |
| **Ecosistema** | Kotlin/Swift | Dart |
| **Comunidad** | Grande | Grande |
| **Hiring** | Difícil | Fácil |
| **Futuro** | ✅ Google-backed | ✅ Google-backed |

**Veredicto:** Flutter más rápido, KMP mejor para apps que requieren UX ultra-pulida.

---

### 4.3 KMP vs React Native

| Aspecto | KMP | React Native |
|---------|-----|-------------|
| **Performance** | Mejor (no JS bridge) | Buena |
| **Tipado** | Estático fuerte | TypeScript |
| **Código compartido** | 65% | 85% |
| **Hot reload** | ✅ | ✅ |
| **Ecosystem** | Kotlin/Swift | JavaScript |
| **Curva aprendizaje** | Alta para nativos | Baja para web devs |
| **WebRTC** | Nativo | react-native-webrtc |
| **Hiring** | Difícil | Fácil |
| **Meta support** | Google | Meta (menos activo) |

**Veredicto:** KMP si tienes equipo Android/iOS, RN si tienes equipo web.

---

## 5. Decisión Final y Recomendaciones

### 5.1 Matriz de Decisión

| Criterio | Peso | KMP | Flutter | RN | Native |
|----------|------|-----|---------|----|----- --|
| Tiempo to market | 25% | 7/10 | 9/10 | 8/10 | 5/10 |
| Costo desarrollo | 20% | 8/10 | 9/10 | 8/10 | 5/10 |
| Performance | 20% | 9/10 | 7/10 | 6/10 | 10/10 |
| Native UX | 15% | 10/10 | 7/10 | 6/10 | 10/10 |
| Mantenibilidad | 10% | 8/10 | 8/10 | 6/10 | 6/10 |
| Hiring | 10% | 5/10 | 8/10 | 9/10 | 7/10 |
| **TOTAL** | 100% | **7.8** | **8.0** | **7.1** | **7.0** |

**Ganador numérico:** Flutter (8.0)

**PERO:** Para este proyecto específico (WhatsApp clone con WebRTC), **KMP es mejor opción** porque:

1. **WebRTC requiere control nativo** - Flutter plugins no dan control completo
2. **UX crítica** - WhatsApp users esperan experiencia ultra-pulida
3. **Plan iOS confirmado** - KMP amortiza inversión
4. **Performance crítica** - Mensajería real-time no tolera lag

### 5.2 Recomendación Final

**✅ ADELANTE CON KMP**

**Condiciones obligatorias:**

1. **POC SignalR exitoso** (Semana 1)
   - Si falla: considerar Flutter o cambiar a polling

2. **POC WebRTC exitoso** (Semana 15)
   - Si falla: usar Stream SDK o Agora

3. **Contratar/capacitar en KMP**
   - Mínimo 1 dev senior con experiencia KMP
   - O plan de upskilling de 2-3 semanas

4. **Buffer en timeline**
   - Estimación: 22 semanas
   - Realista: 26-28 semanas (20% buffer)
   - Deadline: no comprometer antes de semana 4

### 5.3 Red Flags para Abortar

**Si alguno de estos ocurre, reconsiderar approach:**

❌ POC SignalR falla completamente (Semana 1)
❌ POC WebRTC falla completamente (Semana 15)
❌ Performance <30fps en dispositivos mid-range (Semana 10)
❌ >3 semanas de retraso acumulado (en cualquier punto)
❌ No se puede contratar/capacitar dev KMP (antes de inicio)
❌ Presupuesto se reduce >30%

**Plan de contingencia:** Migrar a Flutter (compartir aprendizajes del POC)

---

## 6. Roadmap Detallado de Riesgos

### Semana 1: CRÍTICO - SignalR POC

**Actividades:**
- Día 1-2: Setup proyecto KMP
- Día 3-4: Implementar SignalRClient wrapper
- Día 5: Testing exhaustivo (100 mensajes, reconexión, etc.)

**GO/NO-GO Decision:** Viernes Semana 1

**Criterios GO:**
- ✅ Conexión exitosa
- ✅ Eventos recibidos
- ✅ Reconexión funciona
- ✅ 0 crashes

**Si NO-GO:** Pivotear a long polling, reconsiderar Flutter

---

### Semana 4: MEDIO - Arquitectura validada

**Actividades:**
- Validar estructura de capas
- Confirmar DI funciona
- Probar flows de datos

**Checkpoint:** ¿Domain + Data layers compilados y testeados?

**Si NO:** Refactorizar antes de continuar

---

### Semana 10: MEDIO - Performance check

**Actividades:**
- Profiling con 1000 mensajes
- Memory leaks check
- Scroll performance test

**Criterios:**
- ✅ 60fps en scroll
- ✅ <150MB RAM
- ✅ No memory leaks

**Si NO:** Sprint de optimización (1 semana)

---

### Semana 15: CRÍTICO - WebRTC POC

**Actividades:**
- Implementar WebRTCManager
- Testing P2P local
- Testing P2P 4G

**GO/NO-GO Decision:** Viernes Semana 15

**Criterios GO:**
- ✅ Conexión P2P exitosa
- ✅ Video >20fps
- ✅ Audio sin eco
- ✅ Funciona en 4G

**Si NO-GO:** Evaluar Stream SDK vs Agora vs solo-audio

---

### Semana 22: CRÍTICO - Release readiness

**Actividades:**
- QA completo
- Performance final
- Security audit

**Criterios:**
- ✅ 0 bugs P0
- ✅ <5 bugs P1
- ✅ Performance targets met
- ✅ APK <50MB

**Si NO:** Delay de 1-2 semanas para polish

---

## 7. Conclusión

### KMP es la elección correcta para este proyecto SI:

✅ Tienes plan confirmado de lanzar iOS en próximos 12 meses
✅ Priorizas performance y native UX sobre speed-to-market
✅ Puedes ejecutar POCs obligatorios (SignalR, WebRTC)
✅ Tienes/contratas expertise en KMP
✅ Timeline permite 5.5-6 meses de desarrollo
✅ Presupuesto permite ~$80k de desarrollo

### KMP NO es la elección correcta SI:

❌ Solo necesitas Android (overhead de KMP innecesario)
❌ Timeline <3 meses (usa Flutter)
❌ Equipo sin experiencia mobile (usa Flutter o RN)
❌ Presupuesto muy limitado (usa Flutter)
❌ No puedes ejecutar POCs (demasiado riesgo)

---

**Decisión recomendada:** ✅ **PROCEDER CON KMP** con POCs obligatorios y planes de contingencia claros.

---

**Documento preparado por:** Claude Sonnet 4.5
**Fecha:** 2026-02-14
**Próxima revisión:** Post-POC SignalR (Semana 1)
