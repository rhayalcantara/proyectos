# Propuesta de Arquitectura: WhatsApp Clone con Kotlin Multiplatform (KMP)

**Fecha:** 2026-02-14
**Objetivo:** Desarrollar aplicación Android nativa con arquitectura KMP, compartiendo lógica de negocio con futura app iOS

---

## 1. Resumen Ejecutivo

### 1.1 Enfoque Propuesto
- **Shared Module (commonMain):** 60-70% del código compartido
- **Android UI:** Jetpack Compose con Material 3
- **iOS UI (futuro):** SwiftUI consumiendo módulo compartido
- **Backend existente:** ASP.NET Core + SignalR (sin cambios)

### 1.2 Estimación Inicial
- **Tiempo de desarrollo:** 4-6 meses (1 desarrollador senior KMP)
- **Complejidad:** ALTA (SignalR + WebRTC en KMP es desafiante)
- **Riesgo técnico:** MEDIO-ALTO (dependencias de bibliotecas third-party)
- **Porcentaje de código compartido realista:** 65% (optimista 70%, pesimista 55%)

---

## 2. Arquitectura de Módulos KMP

### 2.1 Estructura de Proyecto Propuesta

```
whatsapp-clone-kmp/
├── shared/                          # Módulo KMP compartido
│   ├── src/
│   │   ├── commonMain/              # 60-70% del código
│   │   │   ├── kotlin/
│   │   │   │   ├── com.whatsappclone/
│   │   │   │   │   ├── domain/             # 100% compartido
│   │   │   │   │   │   ├── models/         # Data classes puras
│   │   │   │   │   │   │   ├── Usuario.kt
│   │   │   │   │   │   │   ├── Mensaje.kt (TipoMensaje, EstadoMensaje)
│   │   │   │   │   │   │   ├── Chat.kt
│   │   │   │   │   │   │   ├── Estado.kt (Stories)
│   │   │   │   │   │   │   ├── Llamada.kt
│   │   │   │   │   │   │   └── Contacto.kt
│   │   │   │   │   │   ├── usecases/       # Casos de uso
│   │   │   │   │   │   │   ├── auth/
│   │   │   │   │   │   │   │   ├── LoginUseCase.kt
│   │   │   │   │   │   │   │   └── RegisterUseCase.kt
│   │   │   │   │   │   │   ├── chat/
│   │   │   │   │   │   │   │   ├── GetChatsUseCase.kt
│   │   │   │   │   │   │   │   ├── SendMessageUseCase.kt
│   │   │   │   │   │   │   │   └── DeleteMessageUseCase.kt
│   │   │   │   │   │   │   ├── call/
│   │   │   │   │   │   │   │   └── InitiateCallUseCase.kt
│   │   │   │   │   │   │   └── status/
│   │   │   │   │   │   │       ├── CreateStatusUseCase.kt
│   │   │   │   │   │   │       └── GetStatusesUseCase.kt
│   │   │   │   │   │   └── repository/     # Interfaces (100% compartido)
│   │   │   │   │   │       ├── IAuthRepository.kt
│   │   │   │   │   │       ├── IChatRepository.kt
│   │   │   │   │   │       ├── IMessageRepository.kt
│   │   │   │   │   │       ├── ICallRepository.kt
│   │   │   │   │   │       ├── IStatusRepository.kt
│   │   │   │   │   │       └── IRealtimeRepository.kt
│   │   │   │   │   │
│   │   │   │   │   ├── data/               # 80% compartido
│   │   │   │   │   │   ├── remote/
│   │   │   │   │   │   │   ├── api/        # Ktor HTTP clients
│   │   │   │   │   │   │   │   ├── AuthApi.kt
│   │   │   │   │   │   │   │   ├── ChatApi.kt
│   │   │   │   │   │   │   │   ├── MessageApi.kt
│   │   │   │   │   │   │   │   ├── ContactApi.kt
│   │   │   │   │   │   │   │   ├── CallApi.kt
│   │   │   │   │   │   │   │   └── StatusApi.kt
│   │   │   │   │   │   │   ├── dto/        # DTOs de red
│   │   │   │   │   │   │   │   ├── AuthDTOs.kt
│   │   │   │   │   │   │   │   ├── ChatDTOs.kt
│   │   │   │   │   │   │   │   ├── MensajeDTOs.kt
│   │   │   │   │   │   │   │   └── EstadoDTOs.kt
│   │   │   │   │   │   │   └── signalr/    # DESAFÍO CRÍTICO
│   │   │   │   │   │   │       ├── SignalRClient.kt (expect)
│   │   │   │   │   │   │       └── SignalRModels.kt
│   │   │   │   │   │   ├── local/
│   │   │   │   │   │   │   ├── database/   # SQLDelight
│   │   │   │   │   │   │   │   ├── WhatsAppDatabase.sq
│   │   │   │   │   │   │   │   ├── ChatQueries.kt
│   │   │   │   │   │   │   │   └── MessageQueries.kt
│   │   │   │   │   │   │   ├── datastore/  # Settings
│   │   │   │   │   │   │   │   └── PreferencesManager.kt (expect)
│   │   │   │   │   │   │   └── cache/
│   │   │   │   │   │   │       └── MessageCache.kt
│   │   │   │   │   │   └── repository/     # Implementaciones
│   │   │   │   │   │       ├── AuthRepositoryImpl.kt
│   │   │   │   │   │       ├── ChatRepositoryImpl.kt
│   │   │   │   │   │       └── RealtimeRepositoryImpl.kt
│   │   │   │   │   │
│   │   │   │   │   ├── core/               # Utilidades compartidas
│   │   │   │   │   │   ├── network/
│   │   │   │   │   │   │   ├── HttpClientFactory.kt
│   │   │   │   │   │   │   ├── TokenInterceptor.kt
│   │   │   │   │   │   │   └── NetworkMonitor.kt (expect)
│   │   │   │   │   │   ├── di/
│   │   │   │   │   │   │   └── Koin.kt     # DI setup
│   │   │   │   │   │   ├── utils/
│   │   │   │   │   │   │   ├── DateTimeUtils.kt
│   │   │   │   │   │   │   ├── FileUtils.kt (expect)
│   │   │   │   │   │   │   └── ValidationUtils.kt
│   │   │   │   │   │   └── result/
│   │   │   │   │   │       └── Result.kt   # Sealed class para errores
│   │   │   │   │   │
│   │   │   │   │   └── presentation/       # ViewModels compartidos (opcional)
│   │   │   │   │       ├── chat/
│   │   │   │   │       │   └── ChatListViewModel.kt
│   │   │   │   │       ├── message/
│   │   │   │   │       │   └── MessageViewModel.kt
│   │   │   │   │       └── status/
│   │   │   │   │           └── StatusViewModel.kt
│   │   │   │   │
│   │   ├── androidMain/             # 15-20% específico Android
│   │   │   ├── kotlin/
│   │   │   │   ├── com.whatsappclone/
│   │   │   │   │   ├── core/
│   │   │   │   │   │   ├── network/
│   │   │   │   │   │   │   └── NetworkMonitor.android.kt
│   │   │   │   │   │   ├── platform/
│   │   │   │   │   │   │   ├── FileUtils.android.kt
│   │   │   │   │   │   │   ├── MediaPicker.android.kt
│   │   │   │   │   │   │   ├── AudioRecorder.android.kt
│   │   │   │   │   │   │   └── CameraCapture.android.kt
│   │   │   │   │   │   └── di/
│   │   │   │   │   │       └── PlatformModule.android.kt
│   │   │   │   │   ├── data/
│   │   │   │   │   │   ├── local/
│   │   │   │   │   │   │   └── PreferencesManager.android.kt
│   │   │   │   │   │   └── remote/
│   │   │   │   │   │       └── signalr/
│   │   │   │   │   │           └── SignalRClient.android.kt
│   │   │   │   │   │               # Wrapper del cliente SignalR Java
│   │   │   │   │   │               # com.microsoft.signalr:signalr
│   │   │   │   │   ├── webrtc/
│   │   │   │   │   │   └── WebRTCManager.android.kt
│   │   │   │   │   │       # org.webrtc:google-webrtc
│   │   │   │   │   └── notification/
│   │   │   │   │       └── PushNotificationManager.android.kt
│   │   │   │   │           # FCM integration
│   │   │   │
│   │   ├── iosMain/                 # 15-20% específico iOS (futuro)
│   │   │   ├── kotlin/
│   │   │   │   ├── com.whatsappclone/
│   │   │   │   │   ├── core/
│   │   │   │   │   │   └── platform/
│   │   │   │   │   │       ├── FileUtils.ios.kt
│   │   │   │   │   │       └── NetworkMonitor.ios.kt
│   │   │   │   │   ├── data/
│   │   │   │   │   │   ├── local/
│   │   │   │   │   │   │   └── PreferencesManager.ios.kt
│   │   │   │   │   │   └── remote/
│   │   │   │   │   │       └── signalr/
│   │   │   │   │   │           └── SignalRClient.ios.kt
│   │   │   │   │   │               # Wrapper de Swift SignalR library
│   │   │   │   │   ├── webrtc/
│   │   │   │   │   │   └── WebRTCManager.ios.kt
│   │   │   │   │   └── notification/
│   │   │   │   │       └── PushNotificationManager.ios.kt
│   │   │   │
│   │   └── commonTest/              # Tests compartidos
│   │       └── kotlin/
│   │           ├── domain/
│   │           │   └── usecases/
│   │           ├── data/
│   │           │   └── repository/
│   │           └── utils/
│   │
│   └── build.gradle.kts
│
├── androidApp/                      # App Android (20-30% del código)
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/
│   │       │   ├── com.whatsappclone.android/
│   │       │   │   ├── MainActivity.kt
│   │       │   │   ├── WhatsAppApplication.kt
│   │       │   │   ├── ui/
│   │       │   │   │   ├── theme/
│   │       │   │   │   │   ├── Color.kt
│   │       │   │   │   │   ├── Theme.kt
│   │       │   │   │   │   └── Type.kt
│   │       │   │   │   ├── navigation/
│   │       │   │   │   │   └── NavGraph.kt
│   │       │   │   │   ├── screens/
│   │       │   │   │   │   ├── auth/
│   │       │   │   │   │   │   ├── LoginScreen.kt
│   │       │   │   │   │   │   └── RegisterScreen.kt
│   │       │   │   │   │   ├── chat/
│   │       │   │   │   │   │   ├── ChatListScreen.kt
│   │       │   │   │   │   │   └── ChatDetailScreen.kt
│   │       │   │   │   │   ├── message/
│   │       │   │   │   │   │   ├── MessageBubble.kt
│   │       │   │   │   │   │   ├── MessageInput.kt
│   │       │   │   │   │   │   └── MediaPreview.kt
│   │       │   │   │   │   ├── status/
│   │       │   │   │   │   │   ├── StatusListScreen.kt
│   │       │   │   │   │   │   └── StatusViewerScreen.kt
│   │       │   │   │   │   ├── call/
│   │       │   │   │   │   │   ├── CallScreen.kt
│   │       │   │   │   │   │   ├── IncomingCallScreen.kt
│   │       │   │   │   │   │   └── CallHistoryScreen.kt
│   │       │   │   │   │   ├── profile/
│   │       │   │   │   │   │   └── ProfileScreen.kt
│   │       │   │   │   │   └── settings/
│   │       │   │   │   │       └── SettingsScreen.kt
│   │       │   │   │   └── components/
│   │       │   │   │       ├── ChatListItem.kt
│   │       │   │   │       ├── MessageBubble.kt
│   │       │   │   │       ├── AudioPlayer.kt
│   │       │   │   │       ├── VideoPlayer.kt
│   │       │   │   │       └── StatusRing.kt
│   │       │   │   ├── viewmodel/
│   │       │   │   │   ├── AuthViewModel.kt
│   │       │   │   │   ├── ChatListViewModel.kt
│   │       │   │   │   ├── MessageViewModel.kt
│   │       │   │   │   ├── CallViewModel.kt
│   │       │   │   │   └── StatusViewModel.kt
│   │       │   │   └── service/
│   │       │   │       ├── ForegroundCallService.kt
│   │       │   │       └── FCMService.kt
│   │       │   └── AndroidManifest.xml
│   │       └── res/
│   └── build.gradle.kts
│
├── iosApp/                          # App iOS (futuro)
│   └── (SwiftUI + shared module)
│
├── gradle/
│   └── libs.versions.toml           # Version catalog
│
├── build.gradle.kts
└── settings.gradle.kts
```

### 2.2 Breakdown de Código Compartido

| Capa | Compartido | Android | iOS | Notas |
|------|------------|---------|-----|-------|
| **Domain Layer** | 100% | 0% | 0% | Models, UseCases, Repository Interfaces |
| **Data Layer - API** | 95% | 5% | 5% | Ktor HTTP, DTOs, mappers |
| **Data Layer - SignalR** | 40% | 60% | 60% | Interfaces compartidas, impl platform-specific |
| **Data Layer - Database** | 95% | 5% | 5% | SQLDelight compartido |
| **Data Layer - Prefs** | 70% | 30% | 30% | DataStore Preferences |
| **WebRTC** | 20% | 80% | 80% | Interfaces compartidas, impl nativa |
| **Media Handling** | 10% | 90% | 90% | expect/actual para cámara, archivos |
| **Push Notifications** | 50% | 50% | 50% | Lógica compartida, FCM/APNs nativo |
| **Presentation (ViewModels)** | 70% | 30% | 30% | Si usas ViewModels compartidos |
| **UI** | 0% | 100% | 100% | Jetpack Compose vs SwiftUI |

**Total estimado: 60-70% de código compartido**

---

## 3. Tecnologías y Bibliotecas

### 3.1 Shared Module (commonMain)

#### Networking
```kotlin
// build.gradle.kts (shared)
[versions]
kotlin = "2.1.0"
ktor = "3.0.3"
kotlinx-coroutines = "1.10.0"
kotlinx-serialization = "1.7.3"
sqldelight = "2.0.2"
koin = "4.0.0"
datetime = "0.6.1"

[libraries]
# Ktor para REST API
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
ktor-client-auth = { module = "io.ktor:ktor-client-auth", version.ref = "ktor" }

# Ktor platform engines
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }      # Android
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }      # iOS

# Serialización
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

# Coroutines
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }

# SQLDelight (database local)
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }

# Koin DI
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }

# Date/Time
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "datetime" }

# Logging
kermit = { module = "co.touchlab:kermit", version = "2.0.5" }
```

#### SignalR Client - DESAFÍO CRÍTICO

**Problema:** No existe cliente SignalR oficial para KMP.

**Opción 1: Wrapper de bibliotecas nativas (RECOMENDADA)**
```kotlin
// commonMain/kotlin/.../data/remote/signalr/SignalRClient.kt
expect class SignalRClient {
    suspend fun connect(url: String, token: String)
    suspend fun disconnect()
    fun on(event: String, handler: (String) -> Unit)
    suspend fun invoke(method: String, vararg args: Any)
    fun onConnectionStateChanged(handler: (ConnectionState) -> Unit)
}

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
}

// androidMain
actual class SignalRClient {
    private val hubConnection: HubConnection by lazy {
        HubConnectionBuilder
            .create(url)
            .withAccessTokenProvider(Single.defer { Single.just(token) })
            .build()
    }

    actual suspend fun connect(url: String, token: String) {
        hubConnection.start().blockingAwait()
    }

    actual suspend fun disconnect() {
        hubConnection.stop().blockingAwait()
    }

    actual fun on(event: String, handler: (String) -> Unit) {
        hubConnection.on(event, { data: String -> handler(data) }, String::class.java)
    }

    actual suspend fun invoke(method: String, vararg args: Any) {
        hubConnection.invoke(method, *args)
    }
}

// Dependencies (androidMain)
dependencies {
    implementation("com.microsoft.signalr:signalr:8.0.0")
}

// iosMain
actual class SignalRClient {
    private var connection: HubConnection? = null

    actual suspend fun connect(url: String, token: String) {
        // Usar Swift SignalR library mediante interop
        // https://github.com/moozzyk/SignalR-Client-Swift
        connection = HubConnectionBuilder(url: url)
            .withAccessToken(token)
            .build()
        try {
            connection?.start()
        } catch (e: Error) {
            throw Exception("SignalR connection failed: ${e.localizedDescription}")
        }
    }

    actual fun on(event: String, handler: (String) -> Unit) {
        connection?.on(method: event) { (data: String) in
            handler(data)
        }
    }
}

// iosMain - CocoaPods/SPM dependency
cocoapods {
    pod("SwiftSignalRClient") {
        version = "~> 0.9.0"
    }
}
```

**Opción 2: Raw WebSocket + SignalR Protocol (COMPLEJO)**
- Implementar protocolo SignalR sobre WebSocket puro
- Ktor WebSocket client es multiplataforma
- Requiere implementar handshake, negociación, heartbeat
- **NO RECOMENDADO** para proyecto real (muy complejo)

**Opción 3: Biblioteca community (arriesgado)**
- No hay opciones maduras en 2026
- Riesgo de abandono de mantenimiento

**DECISIÓN RECOMENDADA:** Opción 1 (expect/actual wrapper)

#### WebRTC - Implementación Platform-Specific

```kotlin
// commonMain/kotlin/.../domain/webrtc/WebRTCManager.kt
expect class WebRTCManager {
    fun initialize()
    fun createOffer(onSdpCreated: (String) -> Unit)
    fun createAnswer(sdpOffer: String, onSdpCreated: (String) -> Unit)
    fun setRemoteDescription(sdp: String)
    fun addIceCandidate(candidate: String)
    fun startLocalVideo(viewId: Any)
    fun startRemoteVideo(viewId: Any)
    fun toggleAudio(enabled: Boolean)
    fun toggleVideo(enabled: Boolean)
    fun hangup()
    fun onIceCandidate(handler: (String) -> Unit)
}

// androidMain
actual class WebRTCManager {
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null

    actual fun initialize() {
        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)

        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()
    }

    actual fun createOffer(onSdpCreated: (String) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                onSdpCreated(sdp.description)
            }
            // ... otros métodos
        }, constraints)
    }

    // Implementación completa usando org.webrtc:google-webrtc
}

// Dependencies (androidMain)
dependencies {
    implementation("io.getstream:stream-webrtc-android:1.1.5")
    // o
    implementation("org.webrtc:google-webrtc:1.0.32006")
}

// iosMain
actual class WebRTCManager {
    private var peerConnectionFactory: RTCPeerConnectionFactory?
    private var peerConnection: RTCPeerConnection?

    actual fun initialize() {
        RTCInitializeSSL()
        peerConnectionFactory = RTCPeerConnectionFactory()
    }

    // Implementación usando WebRTC framework de iOS
}
```

#### Local Database - SQLDelight

```sql
-- shared/src/commonMain/sqldelight/com/whatsappclone/db/WhatsApp.sq

CREATE TABLE Usuario (
    id TEXT PRIMARY KEY NOT NULL,
    numeroTelefono TEXT NOT NULL UNIQUE,
    nombre TEXT NOT NULL,
    fotoPerfil TEXT,
    estado TEXT,
    estaEnLinea INTEGER AS Boolean DEFAULT 0,
    ultimaConexion INTEGER NOT NULL
);

CREATE TABLE Chat (
    id TEXT PRIMARY KEY NOT NULL,
    nombre TEXT,
    esGrupo INTEGER AS Boolean DEFAULT 0,
    imagenGrupo TEXT,
    descripcion TEXT,
    ultimaActualizacion INTEGER NOT NULL
);

CREATE TABLE Mensaje (
    id TEXT PRIMARY KEY NOT NULL,
    chatId TEXT NOT NULL,
    remitenteId TEXT NOT NULL,
    contenido TEXT,
    tipo TEXT NOT NULL, -- Texto, Imagen, Video, Audio, etc.
    urlArchivo TEXT,
    nombreArchivo TEXT,
    tamanoArchivo INTEGER,
    duracionSegundos INTEGER,
    mensajeRespondidoId TEXT,
    fechaEnvio INTEGER NOT NULL,
    eliminado INTEGER AS Boolean DEFAULT 0,
    editado INTEGER AS Boolean DEFAULT 0,
    estadoLocal TEXT NOT NULL, -- Enviado, Entregado, Leido
    FOREIGN KEY (chatId) REFERENCES Chat(id),
    FOREIGN KEY (remitenteId) REFERENCES Usuario(id)
);

CREATE INDEX mensaje_chat_idx ON Mensaje(chatId);
CREATE INDEX mensaje_fecha_idx ON Mensaje(fechaEnvio);

CREATE TABLE Estado (
    id TEXT PRIMARY KEY NOT NULL,
    usuarioId TEXT NOT NULL,
    tipo TEXT NOT NULL, -- Imagen, Video, Texto
    contenido TEXT,
    urlArchivo TEXT,
    fechaCreacion INTEGER NOT NULL,
    fechaExpiracion INTEGER NOT NULL,
    FOREIGN KEY (usuarioId) REFERENCES Usuario(id)
);

-- Queries automáticas generadas por SQLDelight
selectAllChats:
SELECT * FROM Chat ORDER BY ultimaActualizacion DESC;

selectMessagesByChat:
SELECT * FROM Mensaje WHERE chatId = ? ORDER BY fechaEnvio ASC;

insertMessage:
INSERT OR REPLACE INTO Mensaje VALUES ?;

updateMessageStatus:
UPDATE Mensaje SET estadoLocal = ? WHERE id = ?;

deleteMessage:
UPDATE Mensaje SET eliminado = 1 WHERE id = ?;
```

#### Dependency Injection - Koin

```kotlin
// shared/src/commonMain/kotlin/core/di/Koin.kt
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(
        platformModule,      // expect fun platformModule(): Module
        networkModule,
        databaseModule,
        repositoryModule,
        useCaseModule,
        viewModelModule
    )
}

val networkModule = module {
    single { createHttpClient() }
    single { SignalRClient() }
    single<IAuthRepository> { AuthRepositoryImpl(get(), get()) }
}

val databaseModule = module {
    single { createDatabase(get()) }  // expect fun
    single { get<WhatsAppDatabase>().chatQueries }
    single { get<WhatsAppDatabase>().messageQueries }
}

val repositoryModule = module {
    single<IChatRepository> { ChatRepositoryImpl(get(), get(), get()) }
    single<IMessageRepository> { MessageRepositoryImpl(get(), get(), get()) }
    single<IRealtimeRepository> { RealtimeRepositoryImpl(get()) }
}

val useCaseModule = module {
    factory { LoginUseCase(get()) }
    factory { SendMessageUseCase(get(), get()) }
    factory { GetChatsUseCase(get()) }
}

// androidMain/kotlin/core/di/PlatformModule.android.kt
actual fun platformModule() = module {
    single<Context> { androidContext() }
    single { createDatabase(get()) }
    single { WebRTCManager(get()) }
    single { PushNotificationManager(get()) }
}
```

---

## 4. Arquitectura Clean + MVVM

### 4.1 Domain Layer (100% compartido)

```kotlin
// Models - Data classes puras
@Serializable
data class Usuario(
    val id: String,
    val numeroTelefono: String,
    val nombre: String,
    val fotoPerfil: String? = null,
    val estado: String = "Hey there! I'm using WhatsApp Clone",
    val estaEnLinea: Boolean = false,
    val ultimaConexion: Long
)

@Serializable
enum class TipoMensaje {
    Texto, Imagen, Video, Audio, Documento, Ubicacion, Contacto, Sistema
}

@Serializable
enum class EstadoMensaje {
    Enviado, Entregado, Leido
}

@Serializable
data class Mensaje(
    val id: String,
    val chatId: String,
    val remitenteId: String,
    val contenido: String? = null,
    val tipo: TipoMensaje = TipoMensaje.Texto,
    val urlArchivo: String? = null,
    val nombreArchivo: String? = null,
    val tamanoArchivo: Long? = null,
    val duracionSegundos: Int? = null,
    val mensajeRespondidoId: String? = null,
    val fechaEnvio: Long,
    val eliminado: Boolean = false,
    val editado: Boolean = false,
    val estado: EstadoMensaje = EstadoMensaje.Enviado
)

// Use Cases
class SendMessageUseCase(
    private val messageRepository: IMessageRepository,
    private val realtimeRepository: IRealtimeRepository
) {
    suspend operator fun invoke(
        chatId: String,
        contenido: String,
        tipo: TipoMensaje = TipoMensaje.Texto,
        archivo: ByteArray? = null
    ): Result<Mensaje> {
        return try {
            // 1. Subir archivo si existe
            val urlArchivo = archivo?.let {
                messageRepository.uploadFile(it, tipo).getOrNull()
            }

            // 2. Enviar mensaje via API
            val mensaje = messageRepository.sendMessage(
                chatId = chatId,
                contenido = contenido,
                tipo = tipo,
                urlArchivo = urlArchivo
            ).getOrThrow()

            // 3. Guardar en DB local
            messageRepository.saveMessageLocally(mensaje)

            // 4. Enviar via SignalR
            realtimeRepository.sendMessage(mensaje)

            Result.success(mensaje)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Repository Interfaces
interface IMessageRepository {
    suspend fun sendMessage(
        chatId: String,
        contenido: String,
        tipo: TipoMensaje,
        urlArchivo: String? = null
    ): Result<Mensaje>

    suspend fun getMessages(chatId: String, limit: Int = 50): Result<List<Mensaje>>
    suspend fun uploadFile(data: ByteArray, tipo: TipoMensaje): Result<String>
    suspend fun saveMessageLocally(mensaje: Mensaje)
    suspend fun updateMessageStatus(mensajeId: String, estado: EstadoMensaje)
    fun observeMessages(chatId: String): Flow<List<Mensaje>>
}

interface IRealtimeRepository {
    suspend fun connect(token: String)
    suspend fun disconnect()
    fun sendMessage(mensaje: Mensaje)
    fun sendTyping(chatId: String, isTyping: Boolean)
    fun observeMessages(): Flow<Mensaje>
    fun observeTypingIndicators(): Flow<TypingIndicator>
    fun observeOnlineStatus(): Flow<UserStatus>

    // WebRTC
    suspend fun initiateCall(targetUserId: String, callType: CallType, sdpOffer: String)
    suspend fun answerCall(callerId: String, sdpAnswer: String)
    suspend fun rejectCall(callerId: String)
    suspend fun endCall(otherUserId: String)
    suspend fun sendIceCandidate(targetUserId: String, candidate: String)
    fun observeCallOffers(): Flow<CallOffer>
    fun observeCallAnswers(): Flow<CallAnswer>
}
```

### 4.2 Data Layer (80% compartido)

```kotlin
// Repository Implementation
class MessageRepositoryImpl(
    private val messageApi: MessageApi,
    private val database: WhatsAppDatabase,
    private val fileUtils: FileUtils  // expect/actual
) : IMessageRepository {

    override suspend fun sendMessage(
        chatId: String,
        contenido: String,
        tipo: TipoMensaje,
        urlArchivo: String?
    ): Result<Mensaje> = withContext(Dispatchers.IO) {
        try {
            val response = messageApi.sendMessage(
                SendMessageRequest(
                    chatId = chatId,
                    contenido = contenido,
                    tipo = tipo.name,
                    urlArchivo = urlArchivo
                )
            )
            Result.success(response.toModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeMessages(chatId: String): Flow<List<Mensaje>> {
        return database.messageQueries
            .selectMessagesByChat(chatId)
            .asFlow()
            .mapToList()
            .map { it.map { dbMessage -> dbMessage.toModel() } }
    }

    override suspend fun uploadFile(data: ByteArray, tipo: TipoMensaje): Result<String> {
        return fileUtils.uploadFile(data, tipo)  // Platform-specific
    }
}

// API Client (Ktor)
class MessageApi(private val client: HttpClient) {
    suspend fun sendMessage(request: SendMessageRequest): MessageResponse {
        return client.post("/api/messages") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getMessages(chatId: String, limit: Int = 50): List<MessageResponse> {
        return client.get("/api/messages/$chatId") {
            parameter("limit", limit)
        }.body()
    }
}

// SignalR Realtime Repository
class RealtimeRepositoryImpl(
    private val signalRClient: SignalRClient
) : IRealtimeRepository {

    private val _messages = MutableSharedFlow<Mensaje>()
    override fun observeMessages(): Flow<Mensaje> = _messages

    override suspend fun connect(token: String) {
        signalRClient.connect(
            url = "https://tu-backend.com/hubs/chat",
            token = token
        )

        // Escuchar eventos
        signalRClient.on("ReceiveMessage") { json ->
            val mensaje = Json.decodeFromString<Mensaje>(json)
            _messages.emit(mensaje)
        }

        signalRClient.on("MessageStatusUpdated") { json ->
            val update = Json.decodeFromString<MessageStatusUpdate>(json)
            // Actualizar DB local
        }

        signalRClient.on("UserTyping") { json ->
            val typing = Json.decodeFromString<TypingIndicator>(json)
            _typingIndicators.emit(typing)
        }
    }

    override fun sendMessage(mensaje: Mensaje) {
        signalRClient.invoke("SendMessage", mensaje)
    }

    override suspend fun initiateCall(
        targetUserId: String,
        callType: CallType,
        sdpOffer: String
    ) {
        signalRClient.invoke("CallUser", targetUserId, callType.name, sdpOffer)
    }
}
```

### 4.3 Presentation Layer

**Opción A: ViewModels compartidos (60% compartido)**
```kotlin
// shared/src/commonMain/kotlin/presentation/ChatListViewModel.kt
class ChatListViewModel(
    private val getChatsUseCase: GetChatsUseCase,
    private val realtimeRepository: IRealtimeRepository
) : ViewModel() {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadChats()
        observeRealtimeUpdates()
    }

    private fun loadChats() {
        viewModelScope.launch {
            _isLoading.value = true
            getChatsUseCase()
                .onSuccess { _chats.value = it }
                .onFailure { /* handle error */ }
            _isLoading.value = false
        }
    }

    private fun observeRealtimeUpdates() {
        viewModelScope.launch {
            realtimeRepository.observeMessages().collect { mensaje ->
                // Actualizar lista de chats con nuevo mensaje
            }
        }
    }
}
```

**Opción B: ViewModels Android-specific (más control UI)**
```kotlin
// androidApp/src/main/kotlin/viewmodel/ChatListViewModel.kt
class ChatListViewModel(
    private val getChatsUseCase: GetChatsUseCase
) : ViewModel() {

    val chats = getChatsUseCase()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Android-specific: usar SavedStateHandle, etc.
}
```

**RECOMENDACIÓN:** Opción A para máximo código compartido, Opción B para más flexibilidad Android.

---

## 5. Jetpack Compose UI (Android)

```kotlin
// androidApp/src/main/kotlin/ui/screens/chat/ChatListScreen.kt
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = koinViewModel(),
    onChatClick: (String) -> Unit
) {
    val chats by viewModel.chats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WhatsApp Clone") },
                actions = {
                    IconButton(onClick = { /* search */ }) {
                        Icon(Icons.Default.Search, "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* new chat */ }) {
                Icon(Icons.Default.Add, "New Chat")
            }
        }
    ) { padding ->
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(chats, key = { it.id }) { chat ->
                    ChatListItem(
                        chat = chat,
                        onClick = { onChatClick(chat.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatDetailScreen(
    chatId: String,
    viewModel: MessageViewModel = koinViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Messages list
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(messages.reversed(), key = { it.id }) { mensaje ->
                MessageBubble(
                    mensaje = mensaje,
                    isOwnMessage = mensaje.remitenteId == currentUserId
                )
            }
        }

        // Typing indicator
        if (isTyping) {
            TypingIndicator()
        }

        // Message input
        MessageInputBar(
            onSendMessage = { content, tipo, archivo ->
                viewModel.sendMessage(chatId, content, tipo, archivo)
            }
        )
    }
}

@Composable
fun MessageBubble(mensaje: Mensaje, isOwnMessage: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isOwnMessage) Color(0xFF005C4B) else Color(0xFF1F2C34),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                when (mensaje.tipo) {
                    TipoMensaje.Texto -> {
                        Text(mensaje.contenido ?: "", color = Color.White)
                    }
                    TipoMensaje.Imagen -> {
                        AsyncImage(
                            model = mensaje.urlArchivo,
                            contentDescription = null,
                            modifier = Modifier.size(200.dp)
                        )
                    }
                    TipoMensaje.Audio -> {
                        AudioPlayerComposable(url = mensaje.urlArchivo!!)
                    }
                    // ... otros tipos
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = formatTime(mensaje.fechaEnvio),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    if (isOwnMessage) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(estado = mensaje.estado)
                    }
                }
            }
        }
    }
}
```

---

## 6. Características Platform-Specific

### 6.1 Media Handling (expect/actual)

```kotlin
// commonMain
expect class MediaPicker {
    suspend fun pickImage(): ByteArray?
    suspend fun pickVideo(): ByteArray?
    suspend fun pickDocument(): ByteArray?
    suspend fun capturePhoto(): ByteArray?
}

expect class AudioRecorder {
    fun startRecording()
    fun stopRecording(): ByteArray
    fun getRecordingDuration(): Int
}

// androidMain
actual class MediaPicker(private val context: Context) {
    actual suspend fun pickImage(): ByteArray? {
        // Usar ActivityResultContracts.PickVisualMedia
        return suspendCoroutine { continuation ->
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            // Launch activity and get result
        }
    }

    actual suspend fun capturePhoto(): ByteArray? {
        // Usar CameraX o Camera2 API
    }
}

actual class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null

    actual fun startRecording() {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile)
            prepare()
            start()
        }
    }
}
```

### 6.2 Push Notifications

```kotlin
// androidMain
class FCMService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val payload = message.data
        when (payload["type"]) {
            "new_message" -> showMessageNotification(payload)
            "missed_call" -> showCallNotification(payload)
            "status_update" -> showStatusNotification(payload)
        }
    }

    override fun onNewToken(token: String) {
        // Enviar token al backend
        val api = get<PushApi>()
        CoroutineScope(Dispatchers.IO).launch {
            api.registerPushToken(token)
        }
    }
}

// Manifest
<service
    android:name=".FCMService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

---

## 7. Testing Strategy

### 7.1 Shared Tests (commonTest)

```kotlin
// shared/src/commonTest/kotlin/domain/usecases/SendMessageUseCaseTest.kt
class SendMessageUseCaseTest {
    private lateinit var messageRepository: FakeMessageRepository
    private lateinit var realtimeRepository: FakeRealtimeRepository
    private lateinit var useCase: SendMessageUseCase

    @BeforeTest
    fun setup() {
        messageRepository = FakeMessageRepository()
        realtimeRepository = FakeRealtimeRepository()
        useCase = SendMessageUseCase(messageRepository, realtimeRepository)
    }

    @Test
    fun `send text message successfully`() = runTest {
        val result = useCase(
            chatId = "chat1",
            contenido = "Hello",
            tipo = TipoMensaje.Texto
        )

        assertTrue(result.isSuccess)
        assertEquals("Hello", result.getOrNull()?.contenido)
        verify(messageRepository).sendMessage(any(), any(), any())
        verify(realtimeRepository).sendMessage(any())
    }
}
```

### 7.2 Android UI Tests

```kotlin
// androidApp/src/androidTest/kotlin/ChatListScreenTest.kt
@RunWith(AndroidJUnit4::class)
class ChatListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatList_displaysChats() {
        composeTestRule.setContent {
            ChatListScreen(
                viewModel = FakeChatListViewModel(),
                onChatClick = {}
            )
        }

        composeTestRule.onNodeWithText("Chat 1").assertExists()
        composeTestRule.onNodeWithText("Chat 2").assertExists()
    }
}
```

---

## 8. Desafíos y Soluciones

### 8.1 SignalR en KMP

| Desafío | Solución | Riesgo |
|---------|----------|--------|
| No hay cliente oficial KMP | Wrapper expect/actual de bibliotecas nativas | MEDIO - Requiere mantenimiento manual |
| Autenticación JWT en query string | Implementar en ambas plataformas | BAJO |
| Reconnection logic | Implementar en capa compartida sobre wrapper | MEDIO |
| Event handlers con tipos complejos | Usar JSON serialization/deserialization | BAJO |

**Prototipo de validación recomendado (Semana 1-2):**
```kotlin
// Crear POC minimalista de SignalR client
expect class SignalRClientPOC {
    suspend fun connect(url: String)
    fun on(event: String, handler: (String) -> Unit)
    suspend fun invoke(method: String, arg: String)
}

// Probar:
// 1. Conexión con token JWT
// 2. Recibir mensaje de prueba
// 3. Enviar mensaje de prueba
// 4. Manejo de desconexión/reconexión
// 5. Múltiples event handlers
```

### 8.2 WebRTC

| Desafío | Solución | Riesgo |
|---------|----------|--------|
| API completamente diferente entre Android/iOS | expect/actual con interface común | ALTO - Mucho código duplicado |
| ICE candidate exchange | Via SignalR (ya implementado en backend) | BAJO |
| Permisos de cámara/micrófono | Platform-specific | BAJO |
| Video rendering | Platform views (AndroidView/UIViewRepresentable) | MEDIO |

**Decisión arquitectural:**
- WebRTC será 80% platform-specific
- Compartir solo: modelos de datos (SDP, ICE), lógica de estado de llamada
- Implementaciones separadas para Android (org.webrtc) e iOS (WebRTC.framework)

### 8.3 Media Handling

```kotlin
// Abstracción compartida
interface MediaHandler {
    suspend fun pickImage(): MediaFile?
    suspend fun capturePhoto(): MediaFile?
    suspend fun recordAudio(maxDuration: Int): MediaFile?
    suspend fun uploadMedia(file: MediaFile): String
}

data class MediaFile(
    val data: ByteArray,
    val mimeType: String,
    val filename: String,
    val size: Long
)

// Android: ActivityResultContracts + CameraX
// iOS: PHPickerViewController + AVFoundation
```

### 8.4 Offline-First con SQLDelight

```kotlin
// Strategy: Write-through cache
suspend fun sendMessage(mensaje: Mensaje): Result<Mensaje> {
    // 1. Guardar localmente con estado "pending"
    database.insertMessage(mensaje.copy(estado = EstadoMensaje.Enviado))

    // 2. Intentar enviar al servidor
    try {
        val response = api.sendMessage(mensaje)
        database.updateMessage(response.copy(estado = EstadoMensaje.Entregado))
        return Result.success(response)
    } catch (e: Exception) {
        // Mensaje queda en DB con estado "pending", retry later
        return Result.failure(e)
    }
}

// Background sync worker (Android WorkManager)
class MessageSyncWorker : CoroutineWorker() {
    override suspend fun doWork(): Result {
        val pendingMessages = database.getPendingMessages()
        pendingMessages.forEach { mensaje ->
            try {
                api.sendMessage(mensaje)
                database.updateMessageStatus(mensaje.id, EstadoMensaje.Entregado)
            } catch (e: Exception) {
                // Retry later
            }
        }
        return Result.success()
    }
}
```

---

## 9. Alternativa: Compose Multiplatform

### 9.1 Compartir UI también (90% código compartido)

**Ventajas:**
- Compartir screens, componentes, navigation
- Un solo codebase de UI
- Menor tiempo de desarrollo

**Desventajas:**
- Menos "native feeling" en iOS
- Limitaciones en acceso a APIs nativas
- Performance potencialmente inferior a SwiftUI
- Tooling menos maduro que Jetpack Compose puro

**Decisión para este proyecto: NO RECOMENDADO**

**Razones:**
1. WhatsApp requiere UI/UX muy pulida y platform-specific (gestos, animaciones)
2. WebRTC y media handling son muy dependientes de platform
3. iOS users esperan SwiftUI-like experience
4. Riesgo de "uncanny valley" (casi nativo pero no del todo)

**Recomendación:** Usar KMP solo para lógica de negocio, UI nativa en cada plataforma.

---

## 10. Estructura de Tareas y Timeline

### Fase 1: Setup e Infraestructura (4 semanas)

#### Semana 1: Proyecto KMP Base
- [ ] Setup Gradle KMP con version catalogs
- [ ] Configurar módulos: shared, androidApp
- [ ] Implementar Koin DI
- [ ] Configurar CI/CD (GitHub Actions)
- [ ] **POC SignalR** (crítico)

#### Semana 2: Networking Layer
- [ ] Ktor HTTP client con interceptors
- [ ] Auth API (login, register)
- [ ] Token management (refresh, storage)
- [ ] Error handling global
- [ ] Implementar SignalR client (expect/actual)

#### Semana 3: Database Layer
- [ ] SQLDelight schema (Usuario, Chat, Mensaje, Estado)
- [ ] Queries y mappers
- [ ] Migrations
- [ ] Preferences con DataStore

#### Semana 4: Domain Layer
- [ ] Models de dominio
- [ ] Repository interfaces
- [ ] Use Cases básicos (Auth, Chats, Messages)
- [ ] Result wrapper y error types

### Fase 2: Features Core (8 semanas)

#### Semana 5-6: Autenticación y Perfil
- [ ] Login/Register UI (Compose)
- [ ] Profile screen
- [ ] Settings screen
- [ ] Contact list
- [ ] JWT token refresh automático

#### Semana 7-9: Chat y Mensajería
- [ ] Chat list screen
- [ ] Chat detail screen con mensajes
- [ ] Message input (texto, emoji picker)
- [ ] Message bubbles por tipo
- [ ] Status indicators (enviado, entregado, leído)
- [ ] Typing indicators
- [ ] Online status
- [ ] Real-time updates via SignalR

#### Semana 10-11: Media Support
- [ ] Image picker/camera (expect/actual)
- [ ] Image message display
- [ ] Video picker/recording
- [ ] Video player
- [ ] Audio recorder (expect/actual)
- [ ] Audio player con waveform
- [ ] Document picker
- [ ] File upload API

#### Semana 12: Push Notifications
- [ ] FCM integration (Android)
- [ ] Push token registration
- [ ] Notification handling
- [ ] Deep linking

### Fase 3: Features Avanzadas (6 semanas)

#### Semana 13-14: Stories/Status
- [ ] Status creation (image, video, text)
- [ ] Status list screen
- [ ] Status viewer (stories-like)
- [ ] 24h expiration logic
- [ ] View tracking

#### Semana 15-17: Voice/Video Calls (WebRTC)
- [ ] **POC WebRTC Android** (crítico)
- [ ] Call initiation flow
- [ ] Incoming call screen
- [ ] Active call screen
- [ ] Audio/video controls
- [ ] ICE candidate exchange
- [ ] Call history

#### Semana 18: Group Chats
- [ ] Create group
- [ ] Group info/settings
- [ ] Add/remove participants
- [ ] Admin permissions
- [ ] Group messages

### Fase 4: Polish y Optimización (4 semanas)

#### Semana 19-20: Performance
- [ ] Image caching (Coil)
- [ ] Pagination de mensajes
- [ ] Lazy loading
- [ ] Background sync
- [ ] Offline mode improvements

#### Semana 21: Testing
- [ ] Unit tests (shared)
- [ ] Integration tests
- [ ] UI tests (Compose)
- [ ] E2E testing

#### Semana 22: Release Prep
- [ ] ProGuard/R8 optimization
- [ ] App signing
- [ ] Store listing
- [ ] Beta testing

**TOTAL: 22 semanas (5.5 meses) - 1 desarrollador senior**

---

## 11. Estimación de Código Compartido Real

### Por Módulo

| Módulo | LOC Compartido | LOC Android | LOC iOS | % Compartido |
|--------|----------------|-------------|---------|--------------|
| **Domain** | 2,000 | 0 | 0 | 100% |
| **Data - API** | 1,500 | 100 | 100 | 88% |
| **Data - SignalR** | 300 | 500 | 500 | 23% |
| **Data - Database** | 1,200 | 100 | 100 | 86% |
| **Data - Prefs** | 200 | 100 | 100 | 50% |
| **WebRTC** | 200 | 1,000 | 1,000 | 9% |
| **Media Handling** | 100 | 800 | 800 | 6% |
| **Push Notifications** | 150 | 300 | 300 | 20% |
| **Presentation (VM)** | 1,500 | 300 | 300 | 71% |
| **UI** | 0 | 4,000 | 3,500 | 0% |
| **TOTAL** | ~7,150 | ~7,200 | ~6,700 | **~50%** |

**Nota:** Estimación pesimista debido a SignalR y WebRTC. Con mejor abstracción podría llegar a 60-65%.

---

## 12. Riesgos y Mitigaciones

### 12.1 Riesgos Técnicos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| SignalR client no funciona bien | MEDIA | ALTO | POC en semana 1, fallback a polling |
| WebRTC incompatibilidad | MEDIA | ALTO | POC temprano, considerar biblioteca third-party |
| Performance issues en listas grandes | BAJA | MEDIO | Pagination, virtualization, profiling |
| KMP tooling bugs | MEDIA | BAJO | Usar versiones LTS, reportar bugs |
| Dependencias third-party sin KMP | BAJA | MEDIO | Verificar antes, crear wrappers |

### 12.2 Riesgos de Negocio

| Riesgo | Mitigación |
|--------|------------|
| Timeline muy optimista | Buffer de 20%, priorizar MVP |
| Falta experiencia KMP en equipo | Training, pair programming, POCs |
| Cambios en backend | Versionado de API, feature flags |
| iOS app retrasada | Entregar Android primero, validar shared code |

### 12.3 Plan de Contingencia

**Si SignalR no funciona:**
1. Fallback a long polling para mensajes
2. Usar WebSockets nativos + protocolo custom
3. Implementar solo REST API (pérdida de real-time)

**Si WebRTC es muy complejo:**
1. Usar biblioteca third-party (Stream SDK, Agora)
2. Simplificar a solo audio calls primero
3. Postponer video calls a v2.0

---

## 13. Dependencias y Versiones (2026)

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "2.1.0"
agp = "8.8.0"
compose = "1.7.5"
compose-compiler = "1.5.15"
compose-material3 = "1.3.1"
ktor = "3.0.3"
sqldelight = "2.0.2"
koin = "4.0.0"
coroutines = "1.10.0"
serialization = "1.7.3"
datetime = "0.6.1"
napier = "2.7.1"
coil = "2.7.0"
accompanist = "0.36.0"
firebase-bom = "33.7.0"
webrtc = "1.0.32006"
signalr-android = "8.0.0"

[libraries]
# Kotlin
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "datetime" }

# Ktor
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
ktor-client-auth = { module = "io.ktor:ktor-client-auth", version.ref = "ktor" }

# SQLDelight
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }

# Koin
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-androidx-compose = { module = "io.insert-koin:koin-androidx-compose", version.ref = "koin" }

# Android
androidx-core-ktx = { module = "androidx.core:core-ktx", version = "1.15.0" }
androidx-lifecycle-runtime = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version = "2.8.7" }
androidx-lifecycle-viewmodel = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version = "2.8.7" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version = "1.9.3" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version = "2.8.5" }
androidx-datastore = { module = "androidx.datastore:datastore-preferences", version = "1.1.1" }
androidx-work = { module = "androidx.work:work-runtime-ktx", version = "2.10.0" }

# Compose
compose-ui = { module = "androidx.compose.ui:ui", version.ref = "compose" }
compose-material3 = { module = "androidx.compose.material3:material3", version.ref = "compose-material3" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling", version.ref = "compose" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview", version.ref = "compose" }

# Image Loading
coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }

# Logging
napier = { module = "io.github.aakira:napier", version.ref = "napier" }

# Firebase
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebase-bom" }
firebase-messaging = { module = "com.google.firebase:firebase-messaging-ktx" }

# WebRTC
webrtc-android = { module = "org.webrtc:google-webrtc", version.ref = "webrtc" }

# SignalR
signalr-android = { module = "com.microsoft.signalr:signalr", version.ref = "signalr-android" }

# Accompanist
accompanist-permissions = { module = "com.google.accompanist:accompanist-permissions", version.ref = "accompanist" }
accompanist-systemuicontroller = { module = "com.google.accompanist:accompanist-systemuicontroller", version.ref = "accompanist" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
```

---

## 14. Comparación con Alternativas

### 14.1 KMP vs Native Android + Native iOS

| Aspecto | KMP | Full Native |
|---------|-----|-------------|
| Tiempo desarrollo | 5.5 meses Android + 3 meses iOS | 5 meses Android + 5 meses iOS |
| Código compartido | 60-65% | 0% |
| Performance | ~95% native | 100% native |
| Mantenimiento | Más fácil (shared bugs) | Duplicado |
| Curva aprendizaje | Alta (KMP + expect/actual) | Media (solo platform) |
| Tooling | Maduro pero no perfecto | Excelente |
| Costos | Menor a largo plazo | Mayor |

**Conclusión:** KMP vale la pena si planeas iOS en <1 año.

### 14.2 KMP vs Flutter

| Aspecto | KMP | Flutter |
|---------|-----|---------|
| UI nativa | Sí (Compose/SwiftUI) | No (Flutter widgets) |
| Performance | Mejor | Buena |
| Acceso a APIs nativas | Directo (expect/actual) | Via channels |
| Ecosistema | Kotlin/Swift | Dart (menor) |
| Código compartido | 60-65% | 90-95% |
| Feeling nativo | 100% | 85-90% |

**Conclusión:** KMP mejor para apps que requieren UX muy pulida y platform-specific.

### 14.3 KMP vs React Native

| Aspecto | KMP | React Native |
|---------|-----|-------------|
| Performance | Mejor (no JS bridge) | Buena |
| Tipado | Estático (Kotlin) | TypeScript |
| Código compartido | 60-65% | 80-85% |
| Hot reload | Sí | Sí |
| Curva aprendizaje | Media-Alta | Baja-Media |

**Conclusión:** KMP mejor para equipos Kotlin/Android, RN para equipos web.

---

## 15. Recomendación Final

### 15.1 ¿Usar KMP para este proyecto?

**SÍ, RECOMENDADO** si:
- Planeas lanzar iOS en próximos 12 meses
- Tienes/puedes contratar desarrollador con experiencia KMP
- Priorizas performance y native feeling
- Estás dispuesto a invertir en POCs de SignalR/WebRTC

**NO RECOMENDADO** si:
- Solo necesitas Android (overhead innecesario)
- Timeline muy ajustado (<3 meses)
- Equipo sin experiencia Kotlin
- Presupuesto muy limitado (Flutter más rápido)

### 15.2 Arquitectura Recomendada

```
✅ Shared: Domain + Data + Use Cases (60%)
✅ Platform-specific: UI + WebRTC + Media (40%)
❌ NO Compose Multiplatform (demasiado riesgo para este tipo de app)
✅ Clean Architecture con MVVM
✅ Offline-first con SQLDelight
✅ Real-time con SignalR (expect/actual wrapper)
```

### 15.3 Roadmap Sugerido

**MVP (3 meses):**
- Auth
- Chat 1-1
- Text messages
- Image messages
- Push notifications

**v1.0 (5.5 meses):**
- Video/audio messages
- Voice calls
- Stories/Status
- Group chats

**v1.1 (7 meses):**
- Video calls
- Documents
- Advanced features

**iOS (8-11 meses):**
- Reutilizar shared module
- SwiftUI UI
- 3 meses de desarrollo

### 15.4 Próximos Pasos

1. **Semana 1:** Setup proyecto KMP + POC SignalR
2. **Semana 2:** Decisión GO/NO-GO basada en POC
3. **Semana 3-4:** Implementar networking + database layers
4. **Sprint 1 (Mes 2):** MVP de autenticación + chat
5. **Sprint 2 (Mes 3):** Mensajería completa
6. **Sprint 3 (Mes 4):** Media + calls
7. **Sprint 4 (Mes 5):** Polish + testing
8. **Mes 6:** Release Android beta

---

## 16. Conclusión

Kotlin Multiplatform es una **excelente opción** para este proyecto de WhatsApp clone, con las siguientes consideraciones:

**Fortalezas:**
- 60-65% de código compartido realista
- Performance nativa
- Tooling maduro (Gradle, IDE support)
- Ecosistema Kotlin robusto
- Preparado para iOS futuro

**Debilidades:**
- SignalR requiere wrapper custom (riesgo medio)
- WebRTC muy platform-specific (20% shared)
- Curva de aprendizaje expect/actual
- Menos ejemplos que Flutter/RN para apps real-time

**Veredicto:** **Adelante con KMP**, pero con POCs obligatorios de SignalR y WebRTC en primeras 2 semanas para validar viabilidad técnica.

**Estimación final:**
- **Android app:** 5.5 meses (1 dev)
- **iOS app (reutilizando shared):** +3 meses
- **Total code sharing:** 60-65%
- **ROI:** Alto si se hace iOS, Bajo si solo Android

---

**Documento preparado por:** Claude (Anthropic)
**Fecha:** 2026-02-14
**Versión:** 1.0
