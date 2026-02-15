# Plan de Implementacion - App Android Kotlin Nativo

**Fecha:** 2026-02-14
**Decision:** Opcion A - Kotlin Nativo con Jetpack Compose
**Backend existente:** ASP.NET Core + SignalR + WebRTC + JWT + MySQL

---

## 1. Stack Tecnologico

| Componente | Tecnologia | Version |
|-----------|------------|---------|
| **Lenguaje** | Kotlin | 2.0+ |
| **UI** | Jetpack Compose | 1.7+ |
| **Arquitectura** | Clean Architecture + MVVM/MVI | - |
| **DI** | Hilt (Dagger) | 2.51+ |
| **REST API** | Retrofit + OkHttp | 2.11+ |
| **Real-time** | SignalR Java Client (oficial Microsoft) | 8.0.0 |
| **WebRTC** | Stream WebRTC Android | 1.3.10+ |
| **BD Local** | Room | 2.7+ |
| **Preferencias** | DataStore | 1.1+ |
| **Imagenes** | Coil | 3.0+ |
| **Camara** | CameraX | 1.4+ |
| **Push** | Firebase Cloud Messaging | - |
| **Background** | WorkManager | 2.9+ |
| **Testing** | JUnit + MockK + Turbine + Paparazzi | - |
| **Min SDK** | 24 (Android 7.0) | - |
| **Target SDK** | 35 (Android 15) | - |

---

## 2. Arquitectura del Proyecto

### Estructura multi-modulo

```
whatsapp-clone-android/
├── app/                          # Modulo principal (entry point, navegacion, DI)
├── core/
│   ├── network/                  # Retrofit, OkHttp, SignalR client, interceptors
│   ├── database/                 # Room DB, DAOs, entities, DataStore
│   ├── common/                   # Extensions, utils, constantes
│   └── ui/                       # Theme, composables reutilizables, iconos
├── domain/                       # Modelos de dominio, interfaces de repos, use cases
├── feature/
│   ├── auth/                     # Login, registro, splash
│   ├── chats/                    # Lista de chats
│   ├── chat/                     # Ventana de conversacion
│   ├── calls/                    # Llamadas voz/video
│   ├── status/                   # Estados/Stories
│   ├── contacts/                 # Lista de contactos
│   ├── profile/                  # Perfil de usuario
│   └── settings/                 # Configuracion
└── gradle/
    └── libs.versions.toml        # Version catalog
```

### Capas de la arquitectura

```
Presentation (Compose + ViewModels)
    ↓ eventos/estados
Domain (Use Cases + Repository interfaces + Models)
    ↓ implementaciones
Data (Retrofit + SignalR + Room + Mappers)
```

**Patron MVI para UI:**
- `State` — data class inmutable con todo el estado de la pantalla
- `Event` — sealed interface con acciones del usuario
- `ViewModel` — procesa eventos, emite estados via StateFlow

---

## 3. Integracion con el Backend Existente

### 3.1 REST API (Retrofit)

Endpoints a consumir (mapeados del backend ASP.NET Core):

| Endpoint | Metodo | Descripcion |
|----------|--------|-------------|
| `/api/auth/login` | POST | Login con telefono + password |
| `/api/auth/register` | POST | Registro de usuario |
| `/api/chats` | GET | Lista de chats del usuario |
| `/api/chats/{id}` | GET | Detalle de un chat |
| `/api/chats` | POST | Crear chat nuevo |
| `/api/messages/{chatId}` | GET | Mensajes de un chat |
| `/api/messages` | POST | Enviar mensaje |
| `/api/messages/{id}` | PUT | Editar mensaje |
| `/api/messages/{id}` | DELETE | Eliminar mensaje |
| `/api/contacts` | GET/POST/DELETE | CRUD contactos |
| `/api/users/{id}` | GET/PUT | Perfil de usuario |
| `/api/estados` | GET/POST/DELETE | CRUD estados/stories |
| `/api/calls` | GET/POST | Historial de llamadas |
| `/api/push/register` | POST | Registrar token FCM |

**Interceptors de OkHttp:**
- `AuthInterceptor` — inyecta `Bearer {jwt}` en cada request
- `NetworkInterceptor` — detecta estado offline
- `LoggingInterceptor` — solo en debug

### 3.2 SignalR Hub (`/hubs/chat`)

Cliente oficial: `com.microsoft.signalr:signalr:8.0.0`

**Eventos que escucha (del servidor):**

| Evento | Datos | Uso |
|--------|-------|-----|
| `ReceiveMessage` | MessageDto | Nuevo mensaje en tiempo real |
| `UserTyping` | userId, chatId, isTyping | Indicador "escribiendo..." |
| `MessageStatusUpdated` | messageId, status | Entregado / Leido |
| `UserStatusChanged` | userId, isOnline | Online/offline |
| `ReceiveCallOffer` | callerId, sdpOffer, callType | Llamada entrante |
| `ReceiveCallAnswer` | callerId, sdpAnswer | Respuesta a llamada |
| `ReceiveICECandidate` | userId, candidate | Negociacion WebRTC |
| `CallEnded` | userId | Llamada terminada |
| `CallRejected` | userId, reason | Llamada rechazada |
| `ContactUpdated` | contactDto | Contacto actualizado |
| `GroupUpdated` | chatDto | Info de grupo cambio |

**Metodos que invoca (al servidor):**

| Metodo | Parametros | Uso |
|--------|-----------|-----|
| `SendMessage` | mensajeDto | Enviar mensaje |
| `SendTyping` | chatId, isTyping | Notificar que escribe |
| `MessageDelivered` | messageId, chatId | Confirmar entrega |
| `MessageRead` | messageId, chatId | Confirmar lectura |
| `CallUser` | targetUserId, callType, sdpOffer | Iniciar llamada |
| `AnswerCall` | callerId, sdpAnswer | Contestar llamada |
| `RejectCall` | callerId, reason | Rechazar llamada |
| `EndCall` | otherUserId | Colgar |
| `SendICECandidate` | targetUserId, candidate | ICE candidate |

**Conexion con JWT:**
```
HubConnectionBuilder
    .create("https://backend/hubs/chat")
    .withAccessTokenProvider { tokenProvider.getToken() }
    .withTransport(TransportEnum.WEBSOCKETS)
    .build()
```

**Reconexion automatica** con backoff exponencial al perder conexion.

### 3.3 WebRTC (Llamadas)

Libreria: Stream WebRTC Android (`io.getstream:stream-webrtc-android`)

**Flujo de llamada:**
1. Caller crea `PeerConnection` + obtiene stream local (mic + camara)
2. Caller genera SDP offer → envia via SignalR `CallUser()`
3. Receptor recibe `ReceiveCallOffer` → muestra pantalla de llamada entrante
4. Receptor acepta → crea PeerConnection, genera SDP answer → `AnswerCall()`
5. Ambos intercambian ICE candidates via `SendICECandidate()`
6. Conexion peer-to-peer establecida
7. `EndCall()` para terminar

**STUN/TURN:**
- STUN: `stun:stun.l.google.com:19302` (gratis, para la mayoria de conexiones)
- TURN: necesario para NAT estricto (evaluar Twilio/Vonage si hay problemas)

**Features de llamadas:**
- Mute/unmute microfono
- Speaker/earpiece toggle
- Switch camara frontal/trasera
- Picture-in-Picture (Android 8+)
- Full-screen intent para llamadas entrantes (pantalla apagada)
- Cancelacion de eco y supresion de ruido

---

## 4. Almacenamiento Local y Offline

### Room Database

**Entidades principales:**

| Entidad | Campos clave | Proposito |
|---------|-------------|-----------|
| `MessageEntity` | id, chatId, senderId, content, type, status, syncStatus, timestamp | Mensajes con estado de sync |
| `ChatEntity` | id, name, lastMessage, lastMessageTime, unreadCount, isGroup | Lista de chats |
| `ContactEntity` | id, nombre, telefono, avatar, isOnline, lastSeen | Contactos |
| `UserEntity` | id, nombre, telefono, fotoPerfil, estado | Usuario logueado |

**Campo `syncStatus`:** PENDING → SYNCED | FAILED

### Estrategia offline

1. **Optimistic UI** — mensaje aparece inmediatamente en la UI (status: PENDING)
2. **Envio al servidor** — si hay red, envia via REST/SignalR
3. **Si falla** — queda en Room con syncStatus=PENDING
4. **WorkManager** — cada 15 min (o al recuperar red) sincroniza pendientes
5. **Conflictos** — timestamp del servidor es source of truth

### Cache de medios

- **Coil** para imagenes (LRU en memoria + disco)
- **Archivos descargados** en internal storage de la app
- **Limpieza periodica** de cache > 500MB

---

## 5. Push Notifications (FCM)

### Cambios necesarios en el Backend

El backend actualmente usa Web Push (VAPID). Para Android nativo necesita:

1. **Agregar Firebase Admin SDK** al proyecto .NET
2. **Nuevo endpoint** `POST /api/push/register-fcm` para tokens FCM
3. **Enviar notificaciones** via Firebase cuando el usuario esta offline

### En Android

**Tipos de notificacion:**
- `CHANNEL_MESSAGES` — mensajes nuevos (alta prioridad)
- `CHANNEL_CALLS` — llamadas entrantes (maxima prioridad, full-screen intent)
- `CHANNEL_STATUS` — nuevos estados (prioridad normal)
- `CHANNEL_GROUPS` — actividad de grupos (prioridad normal)

**Features:**
- Respuesta directa desde la notificacion (RemoteInput)
- Bubble notifications (Android 11+)
- Agrupacion por chat
- Avatar del contacto en la notificacion
- Sonidos personalizados por canal

---

## 6. Seguridad

| Aspecto | Implementacion |
|---------|---------------|
| Token JWT | EncryptedSharedPreferences (AES256-GCM) |
| Certificate Pinning | OkHttp CertificatePinner (para produccion) |
| Biometrics | BiometricPrompt para desbloquear app |
| Ofuscacion | R8/ProGuard con reglas para DTOs |
| Logs | Removidos en release via ProGuard |
| Screenshots | FLAG_SECURE opcional en pantallas sensibles |

---

## 7. UX y Diseno

- **Material Design 3** con Dynamic Colors (Material You, Android 12+)
- **Tema oscuro/claro** sincronizado con el sistema
- **Edge-to-edge** con barras transparentes
- **Predictive back gesture** (Android 14+)
- **Swipe-to-reply** en burbujas de mensaje
- **Long-press** para menu contextual (copiar, responder, eliminar, reenviar)
- **Animaciones** con AnimatedVisibility, Crossfade, animateContentSize
- **Haptic feedback** en acciones clave
- **LazyColumn** con Paging 3 para listas largas de mensajes

---

## 8. Fases de Desarrollo

### FASE 1: Fundamentos (Semanas 1-6)

**Semana 1-2: Setup y Autenticacion**
- Crear proyecto multi-modulo con Gradle KTS + Version Catalog
- Configurar Hilt, Room, Retrofit, DataStore
- Implementar login/registro consumiendo `/api/auth/*`
- Splash screen con verificacion de token JWT
- Navegacion basica (Auth → Main)
- Theme Material 3 con colores de WhatsApp

**Semana 3-4: Chat Basico**
- Lista de chats (LazyColumn + Room + Retrofit)
- Pantalla de conversacion con burbujas de mensaje
- Envio/recepcion de mensajes de texto via REST
- Barra de entrada con boton de envio
- Pull-to-refresh para cargar mensajes anteriores
- Paginacion con Paging 3

**Semana 5-6: SignalR en Tiempo Real**
- Conectar SignalR con JWT
- Recibir mensajes en tiempo real (ReceiveMessage)
- Indicadores de escritura (UserTyping)
- Estado online/offline de contactos (UserStatusChanged)
- Double check: entregado y leido (MessageStatusUpdated)
- Reconexion automatica con backoff exponencial
- Foreground Service para mantener conexion

**Entregable Fase 1:** App funcional con chat de texto en tiempo real

---

### FASE 2: Multimedia (Semanas 7-10)

**Semana 7-8: Imagenes y Videos**
- CameraX para captura de fotos
- Selector de galeria (PhotoPicker API)
- Compresion automatica de imagenes (>1MB)
- Preview de imagenes en chat (Coil + AsyncImage)
- Grabacion de video corto
- Reproductor de video inline
- Upload multipart con progress bar

**Semana 9-10: Audio y Documentos**
- Grabacion de audio (MediaRecorder)
- Visualizador de onda en burbuja de audio
- Reproductor de audio inline con controles play/pause/seekbar
- Selector de documentos (ActivityResultContracts)
- Preview de archivos (nombre, tamano, tipo)
- Descarga de archivos con progress
- Gestion de permisos (camara, microfono, almacenamiento)

**Entregable Fase 2:** Chat con soporte multimedia completo

---

### FASE 3: Llamadas (Semanas 11-15)

**Semana 11-13: Llamadas de Voz**
- Integrar Stream WebRTC Android
- Senalizacion via SignalR (offer/answer/ICE)
- Pantalla de llamada entrante (full-screen intent)
- Pantalla de llamada activa (timer, avatar, controles)
- Controles: mute, speaker, colgar
- Manejo de AudioFocus (pausar musica del sistema)
- ConnectionService para integracion con sistema de llamadas
- Notificacion persistente durante llamada

**Semana 14-15: Videollamadas**
- Renderizado de video local y remoto (SurfaceViewRenderer)
- Switch camara frontal/trasera
- Picture-in-Picture (PiP) al salir de la pantalla
- Minimizar/maximizar video local
- Deteccion de calidad de red y ajuste de resolucion
- Cancelacion de eco y supresion de ruido

**Entregable Fase 3:** Llamadas de voz y video funcionales

---

### FASE 4: Features Avanzadas (Semanas 16-19)

**Semana 16-17: Estados/Stories**
- Crear estado con foto o texto
- Visualizador de stories (timer progresivo, swipe entre stories)
- Lista de estados de contactos
- Indicadores de "visto por"
- Expiracion automatica a las 24h
- Captura con CameraX o seleccion de galeria

**Semana 18-19: Offline, Sync y Push**
- Cola de mensajes pendientes (Room + syncStatus)
- WorkManager para sincronizacion en background
- Indicador visual de estado de sync en mensajes
- Retry con backoff exponencial
- Firebase Cloud Messaging setup
- Notificaciones de mensajes con respuesta directa
- Notificaciones de llamada con full-screen intent
- Canales de notificacion por tipo

**Entregable Fase 4:** App con paridad funcional completa

---

### FASE 5: Polish y Calidad (Semanas 20-23)

**Semana 20-21: UX y Animaciones**
- Swipe-to-reply en mensajes
- Long-press menus contextuales
- Animaciones de transicion entre pantallas
- Typing indicator animado (tres puntos)
- Dynamic Colors (Material You)
- Tema oscuro/claro
- Edge-to-edge con barras transparentes
- Predictive back gesture
- Haptic feedback

**Semana 22-23: Seguridad y Testing**
- EncryptedSharedPreferences para tokens
- Certificate pinning (produccion)
- Autenticacion biometrica opcional
- R8/ProGuard configuracion
- Unit tests para ViewModels y Use Cases (MockK + Turbine)
- UI tests con Compose Testing
- Integration tests con Room in-memory
- Screenshot tests con Paparazzi
- Target: 70%+ coverage en domain y presentation

**Entregable Fase 5:** App production-ready

---

### FASE 6: Extras (Opcional, Semanas 24-26)

- Bubble notifications (Android 11+)
- Widget de chats recientes (Glance)
- App shortcuts dinamicos (contactos frecuentes)
- Busqueda de mensajes
- Mensajes fijados
- Respuestas con reacciones (emojis)
- Compartir ubicacion
- Contacto como mensaje

---

## 9. Riesgos y Mitigaciones

| Riesgo | Impacto | Mitigacion |
|--------|---------|------------|
| WebRTC NAT traversal falla | Alto | Configurar TURN server (Twilio/Vonage) |
| Race conditions offline/online | Medio | Timestamps del servidor como source of truth |
| Performance con miles de mensajes | Medio | Paging 3 + LazyColumn con keys |
| Tamano APK grande (~25MB con WebRTC) | Bajo | Android App Bundles + ABI splits |
| Battery drain por SignalR persistente | Medio | Foreground Service + reconexion inteligente |
| Backend necesita cambios para FCM | Medio | Planificar sprint de backend en paralelo |
| Permisos denegados (camara, mic) | Bajo | Flujos de permisos claros con explicacion |

---

## 10. Cambios Necesarios en el Backend

Para que la app Android funcione correctamente, el backend necesita:

1. **Firebase Admin SDK** — para enviar push notifications via FCM
2. **Endpoint FCM** — `POST /api/push/register-fcm` para guardar tokens
3. **Paginacion** — si no existe, agregar paginacion a endpoints de mensajes y chats
4. **Upload de archivos** — verificar que multipart upload funciona correctamente
5. **Thumbnails** — generar thumbnails de imagenes/videos para carga rapida (nice to have)

---

## 11. Dependencias Principales (libs.versions.toml)

```toml
[versions]
kotlin = "2.0.21"
compose-bom = "2024.12.01"
hilt = "2.51.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
room = "2.7.0-alpha12"
signalr = "8.0.0"
webrtc = "1.3.10"
coil = "3.0.4"
camerax = "1.4.1"
paging = "3.3.5"
datastore = "1.1.1"
workmanager = "2.10.0"
firebase-bom = "33.7.0"
mockk = "1.13.13"
turbine = "1.2.0"

[libraries]
# Compose
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }

# DI
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }

# Network
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-gson = { module = "com.squareup.retrofit2:converter-gson", version.ref = "retrofit" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }

# Real-time
signalr = { module = "com.microsoft.signalr:signalr", version.ref = "signalr" }

# WebRTC
webrtc-stream = { module = "io.getstream:stream-webrtc-android", version.ref = "webrtc" }

# Database
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }

# Images
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }

# Camera
camerax-core = { module = "androidx.camera:camera-core", version.ref = "camerax" }
camerax-camera2 = { module = "androidx.camera:camera-camera2", version.ref = "camerax" }
camerax-lifecycle = { module = "androidx.camera:camera-lifecycle", version.ref = "camerax" }
camerax-view = { module = "androidx.camera:camera-view", version.ref = "camerax" }

# Testing
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
```
