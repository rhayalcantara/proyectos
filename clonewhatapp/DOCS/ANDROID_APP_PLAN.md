# Plan para Desarrollo de App Android - Clon de WhatsApp

**Fecha de creación**: 2026-02-14
**Arquitectura Backend**: ASP.NET Core + SignalR + WebRTC
**Plataforma**: Android Nativo con Kotlin

---

## 1. TECNOLOGÍAS Y STACK TÉCNICO

### 1.1 UI Framework
**Decisión: Jetpack Compose (100% Compose)**

**Justificación**:
- API declarativa moderna que reduce boilerplate en un 40%
- Mejor performance con LazyColumn para listas de mensajes grandes
- Animaciones fluidas nativas (AnimatedVisibility, Crossfade)
- Material Design 3 con soporte completo para Dynamic Colors (Material You)
- Previews interactivos aceleran el desarrollo
- Compatibilidad con Modifier para gestos complejos (swipe-to-reply)

**Componentes clave**:
- `LazyColumn` con `key()` para listas de chats y mensajes
- `ModalBottomSheet` para compartir archivos
- `Scaffold` con `TopAppBar` y `BottomNavigation`
- `AsyncImage` (Coil) para cargar imágenes de mensajes
- Custom composables: ChatBubble, TypingIndicator, AudioWaveform

### 1.2 Arquitectura
**Decisión: Clean Architecture + MVVM + MVI para UI**

**Capas**:
```
app/
├── data/              # Capa de datos
│   ├── local/         # Room Database, DataStore
│   ├── remote/        # Retrofit, SignalR clients
│   ├── repository/    # Implementaciones de repositorios
│   └── mapper/        # DTOs <-> Domain models
├── domain/            # Lógica de negocio
│   ├── model/         # Entidades del dominio
│   ├── repository/    # Interfaces de repositorios
│   └── usecase/       # Casos de uso (GetMessagesUseCase, SendMessageUseCase)
├── presentation/      # UI Layer
│   ├── chat/          # Feature: Chat
│   │   ├── ChatViewModel.kt
│   │   ├── ChatScreen.kt
│   │   ├── ChatState.kt (sealed class)
│   │   └── ChatEvent.kt (sealed interface)
│   ├── calls/         # Feature: Llamadas
│   └── status/        # Feature: Estados/Stories
└── core/              # Utilidades comunes
    ├── navigation/
    ├── di/            # Módulos de Hilt
    └── utils/
```

**Patrón MVI para UI State**:
```kotlin
// Estado inmutable único
data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val typingUsers: Set<String> = emptySet(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected
)

// Eventos de usuario
sealed interface ChatEvent {
    data class SendMessage(val content: String, val type: MessageType) : ChatEvent
    data class DeleteMessage(val messageId: String) : ChatEvent
    object LoadMoreMessages : ChatEvent
}
```

**Ventajas**:
- Testabilidad: cada capa se prueba independientemente
- Single Source of Truth para el estado de UI
- Separación clara: UI no conoce detalles de red/BD
- Escalabilidad para equipos grandes

### 1.3 Dependency Injection
**Decisión: Hilt (wrapper de Dagger)**

**Módulos principales**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit { /*...*/ }

    @Provides
    @Singleton
    fun provideSignalRClient(): HubConnection { /*...*/ }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase
}
```

### 1.4 Networking
**Decisión: Retrofit + OkHttp + SignalR Java Client**

**REST API (Retrofit)**:
```kotlin
interface WhatsAppApi {
    @POST("api/auth/login")
    suspend fun login(@Body credentials: LoginRequest): LoginResponse

    @GET("api/chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("page") page: Int
    ): PaginatedResponse<MessageDto>

    @Multipart
    @POST("api/messages/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody
    ): FileUploadResponse
}
```

**OkHttp Interceptors**:
- `AuthInterceptor`: Añade Bearer token a todos los requests
- `NetworkConnectionInterceptor`: Detecta offline/online
- `LoggingInterceptor`: Debug en desarrollo (solo)

**SignalR Client**:
```kotlin
class SignalRManager @Inject constructor(
    private val tokenProvider: TokenProvider
) {
    private val hubConnection: HubConnection = HubConnectionBuilder
        .create("https://tubackend.com/hubs/chat")
        .withAccessTokenProvider { tokenProvider.getToken() }
        .withTransport(TransportEnum.WEBSOCKETS)
        .build()

    fun observeMessages(): Flow<Message> = callbackFlow {
        hubConnection.on("ReceiveMessage", { message: MessageDto ->
            trySend(message.toDomain())
        }, MessageDto::class.java)

        hubConnection.start().blockingAwait()

        awaitClose { hubConnection.stop() }
    }

    suspend fun sendTypingIndicator(chatId: String) {
        hubConnection.send("Typing", chatId)
    }
}
```

---

## 2. SIGNALR CLIENT PARA ANDROID

### 2.1 Librería
**Opción seleccionada**: `com.microsoft.signalr:signalr:8.0.0` (oficial)

**Alternativas consideradas**:
- SmartSocket (no soporta ASP.NET Core Hub)
- OkHttp WebSockets + JSON manual (reinventar la rueda)

### 2.2 Integración con Kotlin Coroutines
```kotlin
@Singleton
class ChatRepository @Inject constructor(
    private val signalRManager: SignalRManager,
    private val api: WhatsAppApi,
    private val messageDao: MessageDao
) {
    // Combina datos locales con eventos en tiempo real
    fun observeChat(chatId: String): Flow<List<Message>> = combine(
        messageDao.observeMessages(chatId),
        signalRManager.observeMessages()
            .filter { it.chatId == chatId }
    ) { local, newMessage ->
        local + newMessage
    }
}
```

### 2.3 Manejo de reconexión automática
```kotlin
hubConnection.onClosed { error ->
    viewModelScope.launch {
        delay(5000) // Backoff exponencial en producción
        hubConnection.start()
    }
}
```

### 2.4 Eventos SignalR a soportar
- `ReceiveMessage`: Nuevo mensaje recibido
- `MessageDelivered`: Confirmación de entrega (doble check)
- `MessageRead`: Mensaje leído (doble check azul)
- `Typing`: Usuario escribiendo
- `UserOnline` / `UserOffline`: Estado de conexión
- `CallOffer` / `CallAnswer` / `IceCandidate`: Señalización WebRTC

---

## 3. WEBRTC INTEGRATION

### 3.1 Librería
**Decisión: Stream WebRTC Android (`io.getstream:stream-webrtc-android:1.3.10`)**

**Justificación**:
- Pre-compilado con últimas actualizaciones de WebRTC (Google deprecó el oficial)
- Extensiones Kotlin Coroutines (`addRtcIceCandidate` como suspend fun)
- Soporte para Jetpack Compose
- Mantenido activamente por Stream

**Alternativa**: `io.github.webrtc-sdk:android` (compilación manual complicada)

### 3.2 Arquitectura de llamadas

```kotlin
@HiltViewModel
class CallViewModel @Inject constructor(
    private val webRtcManager: WebRtcManager,
    private val signalRManager: SignalRManager
) : ViewModel() {

    private val peerConnection = webRtcManager.createPeerConnection()

    fun startCall(contactId: String, isVideo: Boolean) {
        viewModelScope.launch {
            // 1. Obtener stream local (cámara + micrófono)
            val localStream = webRtcManager.createLocalStream(isVideo)
            peerConnection.addStream(localStream)

            // 2. Crear oferta SDP
            val offer = peerConnection.createOffer()
            peerConnection.setLocalDescription(offer)

            // 3. Enviar oferta via SignalR
            signalRManager.sendCallOffer(contactId, offer.description)

            // 4. Escuchar respuesta
            signalRManager.observeCallAnswer()
                .filter { it.from == contactId }
                .collect { answer ->
                    peerConnection.setRemoteDescription(answer.sdp)
                }

            // 5. Intercambiar ICE candidates
            peerConnection.onIceCandidate { candidate ->
                signalRManager.sendIceCandidate(contactId, candidate)
            }
        }
    }
}
```

### 3.3 Configuración STUN/TURN
```kotlin
val iceServers = listOf(
    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
    // TURN server para conexiones detrás de NAT estricto
    PeerConnection.IceServer.builder("turn:tu-turn-server.com:3478")
        .setUsername("user")
        .setPassword("pass")
        .createIceServer()
)
```

### 3.4 Características avanzadas

**Picture-in-Picture (PiP)**:
```kotlin
@Composable
fun VideoCallScreen() {
    val activity = LocalContext.current as ComponentActivity

    DisposableEffect(Unit) {
        val pipParams = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(listOf(
                RemoteAction(/* Mute icon */, "Mute", "Mute", mutePendingIntent)
            ))
            .build()

        activity.enterPictureInPictureMode(pipParams)

        onDispose { /* cleanup */ }
    }
}
```

**Reducción de ruido y eco**:
```kotlin
val constraints = MediaConstraints().apply {
    mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
    mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
}
```

---

## 4. OFFLINE SUPPORT Y SINCRONIZACIÓN

### 4.1 Base de datos local: Room

**Esquema de entidades**:
```kotlin
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val type: MessageType,
    val status: MessageStatus, // Enviado, Entregado, Leido
    val timestamp: Long,
    val localPath: String? = null, // Path a archivo local
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus // Synced, Pending, Failed
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lastMessage: String?,
    val lastMessageTime: Long,
    val unreadCount: Int,
    val isGroup: Boolean
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val telefono: String,
    val avatar: String?,
    val estado: String?, // Estado/About
    val isOnline: Boolean,
    val lastSeen: Long?
)
```

**DAOs con Flows**:
```kotlin
@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC")
    fun observeMessages(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE sync_status = :status")
    suspend fun getPendingSyncMessages(status: SyncStatus): List<MessageEntity>
}
```

### 4.2 Work Manager para sincronización

```kotlin
class SyncMessagesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pendingMessages = messageDao.getPendingSyncMessages(SyncStatus.PENDING)

        return try {
            pendingMessages.forEach { message ->
                api.sendMessage(message.toDto())
                messageDao.updateSyncStatus(message.id, SyncStatus.SYNCED)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry() // Reintenta con backoff exponencial
        }
    }
}

// Programar trabajo periódico
val syncWork = PeriodicWorkRequestBuilder<SyncMessagesWorker>(15, TimeUnit.MINUTES)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()
WorkManager.getInstance(context).enqueue(syncWork)
```

### 4.3 Estrategia de caché

**Nivel 1 - Memoria**: LRU Cache para imágenes (Coil maneja esto)
**Nivel 2 - Disco**: Room para mensajes y archivos descargados
**Nivel 3 - Servidor**: Fetch cuando no existe localmente

```kotlin
suspend fun getMessage(messageId: String): Message {
    // 1. Buscar en Room
    messageDao.getMessageById(messageId)?.let { return it.toDomain() }

    // 2. Fetch del servidor
    val remote = api.getMessage(messageId)

    // 3. Guardar en caché
    messageDao.insertMessage(remote.toEntity())

    return remote.toDomain()
}
```

### 4.4 Cola de mensajes pendientes

```kotlin
// UI envía mensaje inmediatamente (optimistic update)
fun sendMessage(chatId: String, content: String) {
    val tempMessage = Message(
        id = UUID.randomUUID().toString(),
        chatId = chatId,
        content = content,
        status = MessageStatus.ENVIADO,
        syncStatus = SyncStatus.PENDING,
        timestamp = System.currentTimeMillis()
    )

    viewModelScope.launch {
        // Insertar en BD local inmediatamente
        messageDao.insertMessage(tempMessage.toEntity())

        // Intentar enviar al servidor
        try {
            val response = api.sendMessage(tempMessage.toDto())
            messageDao.updateMessage(tempMessage.copy(
                id = response.serverId, // ID del servidor
                syncStatus = SyncStatus.SYNCED
            ))
        } catch (e: Exception) {
            // Si falla, WorkManager reintentará después
            Timber.e(e, "Failed to send message")
        }
    }
}
```

---

## 5. PUSH NOTIFICATIONS

### 5.1 Migración de Web Push a FCM

**Backend debe implementar**:
- Endpoint para registrar FCM token: `POST /api/push/register`
- Envío de notificaciones via Firebase Admin SDK

**Android Client**:
```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Enviar token al backend
        viewModelScope.launch {
            api.registerPushToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data

        when (data["type"]) {
            "NEW_MESSAGE" -> showMessageNotification(data)
            "INCOMING_CALL" -> showCallNotification(data)
            "STATUS_UPDATE" -> showStatusNotification(data)
        }
    }

    private fun showMessageNotification(data: Map<String, String>) {
        val chatId = data["chatId"]!!
        val sender = data["senderName"]!!
        val message = data["message"]!!

        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(sender)
            .setContentText(message)
            .setStyle(NotificationCompat.MessagingStyle(Person.Builder().setName("Me").build())
                .addMessage(message, System.currentTimeMillis(), Person.Builder().setName(sender).build())
            )
            .setContentIntent(createChatPendingIntent(chatId))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(chatId.hashCode(), notification)
    }
}
```

### 5.2 Características avanzadas de notificaciones

**Bubble Notifications (Android 11+)**:
```kotlin
val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder()
    .setDesiredHeight(600)
    .setIcon(IconCompat.createWithResource(context, R.drawable.bubble_icon))
    .setIntent(
        PendingIntent.getActivity(
            context, 0, chatIntent, PendingIntent.FLAG_MUTABLE
        )
    )
    .build()

val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
    .setSmallIcon(R.drawable.ic_notification)
    .setBubbleMetadata(bubbleMetadata)
    .build()
```

**Reply directo desde notificación**:
```kotlin
val replyAction = NotificationCompat.Action.Builder(
    R.drawable.ic_send,
    "Reply",
    createReplyPendingIntent(chatId)
).addRemoteInput(
    RemoteInput.Builder("KEY_REPLY")
        .setLabel("Type a message")
        .build()
).build()

notification.addAction(replyAction)
```

**Agrupación de notificaciones**:
```kotlin
// Mensajes individuales con grupo
val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
    .setGroup("messages_group")
    .build()

// Notificación de resumen
val summaryNotification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
    .setGroup("messages_group")
    .setGroupSummary(true)
    .setContentText("5 new messages")
    .build()
```

---

## 6. MEDIA HANDLING

### 6.1 Captura de fotos y videos

**CameraX API**:
```kotlin
@Composable
fun CameraScreen(
    onPhotoCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            }
        }
    )
}
```

### 6.2 Compresión de imágenes

```kotlin
suspend fun compressImage(uri: Uri): File = withContext(Dispatchers.IO) {
    val originalBitmap = BitmapFactory.decodeStream(
        context.contentResolver.openInputStream(uri)
    )

    val compressedFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")

    FileOutputStream(compressedFile).use { out ->
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
    }

    // Resize si es muy grande
    if (compressedFile.length() > 1_000_000) { // 1MB
        val resizedBitmap = Bitmap.createScaledBitmap(
            originalBitmap,
            originalBitmap.width / 2,
            originalBitmap.height / 2,
            true
        )
        FileOutputStream(compressedFile).use { out ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
        }
    }

    compressedFile
}
```

### 6.3 Grabación de audio

```kotlin
class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): File {
        outputFile = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(outputFile!!.absolutePath)
            prepare()
            start()
        }

        return outputFile!!
    }

    fun stopRecording(): File? {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        return outputFile
    }
}
```

### 6.4 Visualizador de audio con onda

```kotlin
@Composable
fun AudioWaveform(
    audioUri: Uri,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val amplitudes = remember { mutableStateListOf<Float>() }

    LaunchedEffect(audioUri) {
        // Extraer amplitudes del archivo de audio
        withContext(Dispatchers.IO) {
            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(context, audioUri, null)
            // ... extraer amplitudes cada 100ms
        }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(60.dp)) {
        amplitudes.forEachIndexed { index, amplitude ->
            val x = (index * 10).toFloat()
            val height = amplitude * size.height
            drawLine(
                color = MaterialTheme.colorScheme.primary,
                start = Offset(x, size.height / 2 - height / 2),
                end = Offset(x, size.height / 2 + height / 2),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}
```

### 6.5 Selector de archivos

```kotlin
val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri: Uri? ->
    uri?.let { onFileSelected(it) }
}

Button(onClick = { launcher.launch("*/*") }) {
    Text("Seleccionar archivo")
}
```

---

## 7. SEGURIDAD

### 7.1 Almacenamiento de tokens

**EncryptedSharedPreferences**:
```kotlin
class SecureTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        encryptedPrefs.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? {
        return encryptedPrefs.getString("jwt_token", null)
    }
}
```

### 7.2 Certificate Pinning

```kotlin
val certificatePinner = CertificatePinner.Builder()
    .add("tubackend.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    .build()

val okHttpClient = OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    .build()
```

### 7.3 Autenticación biométrica

```kotlin
@Composable
fun BiometricAuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val biometricPrompt = remember {
        BiometricPrompt(
            context as FragmentActivity,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onAuthSuccess()
                }
            }
        )
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Desbloquear WhatsApp")
        .setSubtitle("Usa tu huella digital")
        .setNegativeButtonText("Cancelar")
        .build()

    LaunchedEffect(Unit) {
        biometricPrompt.authenticate(promptInfo)
    }
}
```

### 7.4 Ofuscación de código

**ProGuard/R8 rules**:
```proguard
# Ofuscar todas las clases excepto modelos de red
-keep class com.tuapp.data.remote.dto.** { *; }

# Remover logs en producción
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
```

---

## 8. UX Y DISEÑO ANDROID-SPECIFIC

### 8.1 Material Design 3

**Dynamic Colors (Material You)**:
```kotlin
@Composable
fun WhatsAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = Color(0xFF00A884), // Verde WhatsApp
            secondary = Color(0xFF25D366)
        )
        else -> lightColorScheme(
            primary = Color(0xFF075E54),
            secondary = Color(0xFF128C7E)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### 8.2 Edge-to-edge design

```kotlin
@Composable
fun ChatScreen() {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(), // Respeta status bar y navigation bar
        topBar = {
            TopAppBar(
                title = { Text("Chat Name") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        // Contenido
    }
}
```

### 8.3 Predictive Back Gesture (Android 14+)

```kotlin
val backCallback = remember {
    object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Animación personalizada al retroceder
            navController.popBackStack()
        }
    }
}

val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

DisposableEffect(backDispatcher) {
    backDispatcher?.addCallback(backCallback)
    onDispose { backCallback.remove() }
}
```

### 8.4 Gestos personalizados

**Swipe-to-reply**:
```kotlin
@Composable
fun MessageBubble(
    message: Message,
    onReply: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.toInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > 100f) {
                            onReply()
                        }
                        offsetX = 0f
                    }
                ) { _, dragAmount ->
                    offsetX = (offsetX + dragAmount).coerceIn(0f, 150f)
                }
            }
    ) {
        // Contenido del mensaje
    }
}
```

### 8.5 Animaciones fluidas

```kotlin
@Composable
fun TypingIndicator(isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Row {
            repeat(3) { index ->
                val infiniteTransition = rememberInfiniteTransition()
                val offset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = -10f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(300, delayMillis = index * 100),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(y = offset.dp)
                        .background(Color.Gray, CircleShape)
                )
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}
```

---

## 9. ESTRUCTURA DE MÓDULOS

### 9.1 Multi-module architecture

```
whatsapp-clone/
├── app/                      # App module (punto de entrada)
├── core/
│   ├── network/              # Retrofit, SignalR, OkHttp
│   ├── database/             # Room, DataStore
│   ├── common/               # Utils, extensions
│   └── ui/                   # Composables reutilizables
├── feature/
│   ├── auth/                 # Login, registro
│   ├── chats/                # Lista de chats
│   ├── chat/                 # Pantalla de conversación
│   ├── calls/                # Llamadas de voz/video
│   ├── status/               # Estados/Stories
│   ├── contacts/             # Lista de contactos
│   └── settings/             # Configuración
└── domain/                   # Modelos de dominio, use cases
```

**Beneficios**:
- Compilación paralela más rápida
- Encapsulación: features no se conocen entre sí
- Reutilización: `core` se usa en todas las features
- Equipos pueden trabajar en features independientemente

### 9.2 Build configuration

**Version Catalog (libs.versions.toml)**:
```toml
[versions]
compose = "1.7.0"
hilt = "2.51"
retrofit = "2.11.0"
room = "2.7.0"
signalr = "8.0.0"
webrtc = "1.3.10"

[libraries]
androidx-compose-ui = { module = "androidx.compose.ui:ui", version.ref = "compose" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
signalr = { module = "com.microsoft.signalr:signalr", version.ref = "signalr" }
webrtc-stream = { module = "io.getstream:stream-webrtc-android", version.ref = "webrtc" }

[plugins]
android-application = { id = "com.android.application", version = "8.5.0" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version = "2.0.0" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

**app/build.gradle.kts**:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    id("kotlin-kapt")
}

android {
    namespace = "com.tuapp.whatsapp"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.7.0"
    }
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":feature:chats"))

    implementation(libs.androidx.compose.ui)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

---

## 10. TESTING STRATEGY

### 10.1 Unit Tests (JUnit + MockK)

```kotlin
@ExperimentalCoroutinesApi
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: ChatViewModel
    private val chatRepository: ChatRepository = mockk()

    @Before
    fun setup() {
        viewModel = ChatViewModel(chatRepository)
    }

    @Test
    fun `sendMessage should add message to state`() = runTest {
        // Given
        val message = "Hello"
        coEvery { chatRepository.sendMessage(any()) } returns Result.success(Unit)

        // When
        viewModel.onEvent(ChatEvent.SendMessage(message))

        // Then
        val state = viewModel.state.value
        assertTrue(state.messages.any { it.content == message })
    }
}
```

### 10.2 UI Tests (Compose Testing)

```kotlin
@HiltAndroidTest
class ChatScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun clickSendButton_shouldSendMessage() {
        composeTestRule.apply {
            onNodeWithTag("message_input").performTextInput("Test message")
            onNodeWithTag("send_button").performClick()

            onNodeWithText("Test message").assertIsDisplayed()
        }
    }
}
```

### 10.3 Integration Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class MessageSyncIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: ChatRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()

        repository = ChatRepository(database.messageDao(), mockApi, mockSignalR)
    }

    @Test
    fun offlineMessage_shouldSyncWhenOnline() = runBlocking {
        // Enviar mensaje offline
        repository.sendMessage(Message(/* ... */))

        // Verificar que está en estado PENDING
        val pending = database.messageDao().getPendingSyncMessages()
        assertEquals(1, pending.size)

        // Simular conexión
        repository.syncPendingMessages()

        // Verificar que está SYNCED
        val synced = database.messageDao().getPendingSyncMessages()
        assertEquals(0, synced.size)
    }
}
```

### 10.4 Screenshot Tests (Paparazzi)

```kotlin
class ChatScreenScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun chatScreen_withMessages() {
        paparazzi.snapshot {
            WhatsAppTheme {
                ChatScreen(
                    state = ChatState(
                        messages = listOf(
                            Message(/* ... */),
                            Message(/* ... */)
                        )
                    ),
                    onEvent = {}
                )
            }
        }
    }
}
```

---

## 11. CARACTERÍSTICAS AVANZADAS

### 11.1 Widget de inicio

```kotlin
@Composable
fun ChatWidget() {
    GlanceTheme {
        Column {
            Text("Chats recientes")
            LazyColumn {
                items(recentChats) { chat ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionStartActivity<MainActivity>())
                    ) {
                        Image(provider = ImageProvider(chat.avatar))
                        Text(chat.name)
                    }
                }
            }
        }
    }
}
```

### 11.2 Wear OS companion

```kotlin
// Mobile app envia mensajes a reloj
val messageClient = Wearable.getMessageClient(context)
messageClient.sendMessage(
    nodeId,
    "/new_message",
    messageData.toByteArray()
)

// Wear OS recibe y muestra notificación
override fun onMessageReceived(messageEvent: MessageEvent) {
    if (messageEvent.path == "/new_message") {
        showNotification(String(messageEvent.data))
    }
}
```

### 11.3 App Shortcuts

```kotlin
val shortcut = ShortcutInfoCompat.Builder(context, "chat_${contact.id}")
    .setShortLabel(contact.name)
    .setLongLabel("Chat with ${contact.name}")
    .setIcon(IconCompat.createWithBitmap(contact.avatar))
    .setIntent(
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("chat_id", contact.id)
        }
    )
    .build()

ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
```

### 11.4 Adaptive Icons

```xml
<!-- res/mipmap-anydpi-v26/ic_launcher.xml -->
<adaptive-icon>
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>
</adaptive-icon>
```

---

## 12. COMPLEJIDAD Y TIMELINE ESTIMADO

### Fase 1: Fundamentos (4-6 semanas)
- **Semana 1-2**: Setup del proyecto, arquitectura base, módulos
  - Configurar Hilt, Room, Retrofit
  - Implementar autenticación (login/registro)
  - Pantalla de splash con verificación de token

- **Semana 3-4**: Chat básico
  - Lista de chats con Room
  - Pantalla de conversación con Compose
  - Envío/recepción de mensajes de texto via REST

- **Semana 5-6**: SignalR integration
  - Mensajes en tiempo real
  - Indicadores de escritura
  - Estados online/offline
  - Double check (entregado/leído)

**Entregable**: App funcional con chat de texto básico

### Fase 2: Multimedia (3-4 semanas)
- **Semana 7-8**: Media handling
  - CameraX para fotos/videos
  - Selector de galería
  - Compresión de imágenes
  - Previsualización de imágenes en chat

- **Semana 9-10**: Audio y documentos
  - Grabación de audio con visualización de onda
  - Reproductor de audio inline
  - Soporte para documentos (PDF, DOC, etc.)
  - Progress bar para uploads/downloads

**Entregable**: Chat con soporte completo de multimedia

### Fase 3: Llamadas (4-5 semanas)
- **Semana 11-13**: WebRTC voice calls
  - Integrar Stream WebRTC
  - Señalización via SignalR
  - Pantalla de llamada entrante (full-screen intent)
  - Control de audio (mute, speaker)

- **Semana 14-15**: Video calls
  - Renderizado de video local/remoto
  - Switch cámara frontal/trasera
  - Picture-in-Picture
  - Optimización de calidad según red

**Entregable**: Llamadas de voz y video funcionales

### Fase 4: Features avanzadas (3-4 semanas)
- **Semana 16-17**: Status/Stories
  - Subir status con foto/video
  - Visualizador con timer de 24h
  - Indicadores de visto
  - Expiración automática

- **Semana 18-19**: Offline & Sync
  - Cola de mensajes pendientes
  - WorkManager para sincronización
  - Indicadores de estado de sincronización
  - Retry logic robusto

**Entregable**: App con paridad de features vs WhatsApp

### Fase 5: Polish y seguridad (2-3 semanas)
- **Semana 20-21**: UX refinement
  - Animaciones fluidas
  - Gestos (swipe-to-reply, long-press)
  - Material You theming
  - Adaptive layouts para tablets

- **Semana 22**: Security & Testing
  - Certificate pinning
  - Biometric authentication
  - ProGuard configuration
  - E2E testing suite

**Entregable**: App production-ready

### Fase 6 (Opcional): Features premium (2-3 semanas)
- Widget de inicio
- Wear OS companion
- Bubble notifications
- App shortcuts dinámicos
- Themes personalizados

---

## TOTAL ESTIMADO: 18-23 semanas (4.5-6 meses)

**Team ideal**:
- 2 Android developers (1 senior, 1 mid)
- 1 QA engineer
- 1 designer (part-time)

**Riesgos y desafíos**:

1. **WebRTC complejidad**: NAT traversal, TURN server costs
   - Mitigación: Usar TURN server confiable (Twilio, Vonage)

2. **Sincronización offline/online**: Race conditions
   - Mitigación: Usar timestamps del servidor + conflict resolution

3. **Performance con miles de mensajes**: LazyColumn lag
   - Mitigación: Paginación + Paging 3 library

4. **Tamaño del APK**: WebRTC añade ~10MB
   - Mitigación: Android App Bundles + splits por ABI

5. **Push notifications en China**: FCM bloqueado
   - Mitigación: Integrar proveedores locales (Huawei Push)

6. **Battery drain**: SignalR mantiene conexión persistente
   - Mitigación: Usar JobScheduler para reconexión inteligente

7. **Backend changes**: SignalR Hub puede cambiar
   - Mitigación: Versioning de API, contract testing

---

## DIFERENCIADORES PREMIUM

1. **Transcripción de audio**: Speech-to-text con ML Kit
2. **Smart replies**: Sugerencias contextuales con Gemini Nano (on-device)
3. **Translation inline**: Traducir mensajes sin salir del chat
4. **Disappearing messages**: Mensajes que se autodestruyen
5. **Polls**: Encuestas en grupos
6. **Live location sharing**: Compartir ubicación en tiempo real (5min/1h/8h)
7. **Dark theme auto**: Cambiar según hora del día
8. **Backup to Google Drive**: Respaldo automático cifrado
9. **Fingerprint lock**: Proteger app con huella/PIN
10. **Custom notifications**: Tonos personalizados por chat

---

## CONCLUSIÓN

Este plan propone una aplicación Android nativa de alta calidad que aprovecha las últimas tecnologías de Google (Jetpack Compose, Material You, CameraX) combinadas con bibliotecas probadas de la comunidad (Hilt, Retrofit, Stream WebRTC).

La arquitectura Clean + MVVM garantiza mantenibilidad a largo plazo, mientras que el soporte offline robusto asegura una UX fluida incluso en condiciones de red pobres.

El enfoque modular permite iterar rápidamente y agregar features sin romper la base existente.

**Próximos pasos sugeridos**:
1. Crear prototipo de UI en Figma
2. Setup del proyecto con módulos base
3. Implementar autenticación + pantalla de chats
4. Spike técnico de SignalR + WebRTC

---

**Documentos de referencia**:
- [SignalR Android Implementation](https://proandroiddev.com/signalr-android-tutorial-fe2302b8bbca)
- [Stream WebRTC Android](https://github.com/GetStream/webrtc-android)
- [Jetpack Compose Chat Architecture](https://github.com/zeeshanali-k/Gup)

