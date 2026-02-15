# Plan de Tareas: WhatsApp Clone con Kotlin Multiplatform

**Proyecto:** WhatsApp Clone - Android App con KMP
**Fecha inicio:** 2026-02-14
**Duración estimada:** 22 semanas (5.5 meses)
**Desarrolladores:** 1 senior KMP

---

## Estado del Plan

**Fase actual:** Planificación
**Progreso general:** 0%
**Próximo milestone:** POC SignalR (Semana 1)

---

## Fase 1: Setup e Infraestructura (4 semanas)

### Semana 1: Proyecto KMP Base y POC Crítico

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| 1.1 | Crear estructura de proyecto KMP (shared + androidApp) | ⏸️ Pendiente | 🔴 CRÍTICA | Gradle 8.8, Kotlin 2.1 |
| 1.2 | Configurar build.gradle.kts con version catalog | ⏸️ Pendiente | 🔴 CRÍTICA | libs.versions.toml |
| 1.3 | Setup commonMain/androidMain/iosMain | ⏸️ Pendiente | 🔴 CRÍTICA | Estructura de carpetas |
| 1.4 | Configurar Koin DI básico | ⏸️ Pendiente | 🟡 ALTA | networkModule, platformModule |
| 1.5 | **POC: SignalR Client** (expect/actual) | ⏸️ Pendiente | 🔴 CRÍTICA | GO/NO-GO decision |
| 1.6 | POC: Conectar a /hubs/chat con JWT | ⏸️ Pendiente | 🔴 CRÍTICA | Validar viabilidad |
| 1.7 | POC: Recibir/enviar mensaje de prueba | ⏸️ Pendiente | 🔴 CRÍTICA | Event handlers |
| 1.8 | POC: Manejo de reconexión | ⏸️ Pendiente | 🟡 ALTA | ConnectionState |
| 1.9 | Setup CI/CD (GitHub Actions) | ⏸️ Pendiente | 🟢 MEDIA | Build + tests |
| 1.10 | Decisión GO/NO-GO en SignalR | ⏸️ Pendiente | 🔴 CRÍTICA | Viernes semana 1 |

**Criterios de aceptación Semana 1:**
- ✅ Proyecto compila en Android
- ✅ Shared module funciona
- ✅ SignalR POC conecta y recibe mensajes
- ✅ Decisión documentada sobre viabilidad

---

### Semana 2: Networking Layer

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| 2.1 | Configurar Ktor HttpClient (OkHttp engine) | ⏸️ Pendiente | 🔴 CRÍTICA | Content negotiation |
| 2.2 | Implementar TokenInterceptor para JWT | ⏸️ Pendiente | 🔴 CRÍTICA | Bearer header |
| 2.3 | Implementar AuthApi (login, register) | ⏸️ Pendiente | 🔴 CRÍTICA | /api/auth endpoints |
| 2.4 | Crear DTOs de autenticación | ⏸️ Pendiente | 🔴 CRÍTICA | LoginDTO, RegisterDTO, AuthResponseDTO |
| 2.5 | Implementar token storage (expect/actual) | ⏸️ Pendiente | 🟡 ALTA | DataStore preferences |
| 2.6 | Implementar token refresh automático | ⏸️ Pendiente | 🟡 ALTA | RefreshTokenInterceptor |
| 2.7 | Error handling global (Result wrapper) | ⏸️ Pendiente | 🟡 ALTA | sealed class Result |
| 2.8 | Implementar SignalRClient completo | ⏸️ Pendiente | 🔴 CRÍTICA | Basado en POC |
| 2.9 | Network monitor (expect/actual) | ⏸️ Pendiente | 🟢 MEDIA | Detectar online/offline |
| 2.10 | Logging con Napier | ⏸️ Pendiente | 🟢 BAJA | Debug requests |

**Criterios de aceptación Semana 2:**
- ✅ Login/register funciona via Ktor
- ✅ Token se guarda y se usa en requests
- ✅ SignalR conecta con token
- ✅ Manejo de errores centralizado

---

### Semana 3: Database Layer

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| 3.1 | Configurar SQLDelight plugin | ⏸️ Pendiente | 🔴 CRÍTICA | build.gradle.kts |
| 3.2 | Crear schema: tabla Usuario | ⏸️ Pendiente | 🔴 CRÍTICA | WhatsApp.sq |
| 3.3 | Crear schema: tabla Chat | ⏸️ Pendiente | 🔴 CRÍTICA | Incluir campos grupo |
| 3.4 | Crear schema: tabla Mensaje | ⏸️ Pendiente | 🔴 CRÍTICA | Con tipos y estados |
| 3.5 | Crear schema: tabla Estado (stories) | ⏸️ Pendiente | 🟡 ALTA | Con expiración |
| 3.6 | Implementar queries: selectAllChats | ⏸️ Pendiente | 🔴 CRÍTICA | ORDER BY ultimaActualizacion |
| 3.7 | Implementar queries: selectMessagesByChat | ⏸️ Pendiente | 🔴 CRÍTICA | Pagination |
| 3.8 | Implementar queries: insertMessage | ⏸️ Pendiente | 🔴 CRÍTICA | INSERT OR REPLACE |
| 3.9 | Implementar queries: updateMessageStatus | ⏸️ Pendiente | 🟡 ALTA | Enviado/Entregado/Leído |
| 3.10 | Crear database driver (expect/actual) | ⏸️ Pendiente | 🔴 CRÍTICA | AndroidSqliteDriver |
| 3.11 | Implementar migrations | ⏸️ Pendiente | 🟡 ALTA | Schema version 1 |
| 3.12 | Setup DataStore Preferences | ⏸️ Pendiente | 🟡 ALTA | User settings |
| 3.13 | Crear mappers DB ↔ Domain models | ⏸️ Pendiente | 🔴 CRÍTICA | Extension functions |

**Criterios de aceptación Semana 3:**
- ✅ Database se crea correctamente
- ✅ Queries funcionan con Flow
- ✅ Mappers convierten correctamente
- ✅ Migrations probadas

---

### Semana 4: Domain Layer

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| 4.1 | Crear data classes: Usuario | ⏸️ Pendiente | 🔴 CRÍTICA | @Serializable |
| 4.2 | Crear data classes: Chat | ⏸️ Pendiente | 🔴 CRÍTICA | Con tipo (individual/grupo) |
| 4.3 | Crear data classes: Mensaje | ⏸️ Pendiente | 🔴 CRÍTICA | Con TipoMensaje enum |
| 4.4 | Crear enums: TipoMensaje | ⏸️ Pendiente | 🔴 CRÍTICA | Texto, Imagen, Video, etc. |
| 4.5 | Crear enums: EstadoMensaje | ⏸️ Pendiente | 🔴 CRÍTICA | Enviado, Entregado, Leído |
| 4.6 | Crear data classes: Estado (story) | ⏸️ Pendiente | 🟡 ALTA | Con expiración |
| 4.7 | Crear interfaces: IAuthRepository | ⏸️ Pendiente | 🔴 CRÍTICA | login, register, logout |
| 4.8 | Crear interfaces: IChatRepository | ⏸️ Pendiente | 🔴 CRÍTICA | getChats, createChat |
| 4.9 | Crear interfaces: IMessageRepository | ⏸️ Pendiente | 🔴 CRÍTICA | sendMessage, getMessages |
| 4.10 | Crear interfaces: IRealtimeRepository | ⏸️ Pendiente | 🔴 CRÍTICA | SignalR events |
| 4.11 | Implementar UseCase: LoginUseCase | ⏸️ Pendiente | 🔴 CRÍTICA | Validación + llamada API |
| 4.12 | Implementar UseCase: RegisterUseCase | ⏸️ Pendiente | 🔴 CRÍTICA | Validación teléfono |
| 4.13 | Implementar UseCase: GetChatsUseCase | ⏸️ Pendiente | 🔴 CRÍTICA | Merge DB + API |
| 4.14 | Implementar UseCase: SendMessageUseCase | ⏸️ Pendiente | 🔴 CRÍTICA | Upload + send + SignalR |
| 4.15 | Implementar Result sealed class | ⏸️ Pendiente | 🟡 ALTA | Success, Error, Loading |

**Criterios de aceptación Semana 4:**
- ✅ Domain models definidos
- ✅ Repository interfaces documentadas
- ✅ Use cases con lógica de negocio
- ✅ Código 100% compartido (no platform specific)

---

## Fase 2: Features Core (8 semanas)

### Semana 5-6: Autenticación y Perfil

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| 5.1 | Setup Jetpack Compose (androidApp) | ⏸️ Pendiente | 🔴 CRÍTICA | Material 3 |
| 5.2 | Crear tema (colors, typography) | ⏸️ Pendiente | 🟡 ALTA | WhatsApp-like |
| 5.3 | Crear LoginScreen (Compose) | ⏸️ Pendiente | 🔴 CRÍTICA | Phone input + validación |
| 5.4 | Crear RegisterScreen (Compose) | ⏸️ Pendiente | 🔴 CRÍTICA | Name + phone + password |
| 5.5 | Implementar AuthViewModel | ⏸️ Pendiente | 🔴 CRÍTICA | StateFlow para UI state |
| 5.6 | Implementar navigation graph | ⏸️ Pendiente | 🔴 CRÍTICA | NavHost + routes |
| 5.7 | Implementar splash screen | ⏸️ Pendiente | 🟢 MEDIA | Auto-login si token válido |
| 5.8 | Crear ProfileScreen | ⏸️ Pendiente | 🟡 ALTA | Editar nombre, foto, estado |
| 5.9 | Implementar image picker (expect/actual) | ⏸️ Pendiente | 🟡 ALTA | Para foto perfil |
| 5.10 | Implementar upload de imagen de perfil | ⏸️ Pendiente | 🟡 ALTA | API endpoint |
| 5.11 | Crear SettingsScreen | ⏸️ Pendiente | 🟢 MEDIA | Theme, notifications, etc. |
| 5.12 | Implementar ContactListScreen | ⏸️ Pendiente | 🟡 ALTA | Con permisos de contactos |
| 5.13 | Implementar token refresh UI flow | ⏸️ Pendiente | 🟡 ALTA | Logout si refresh falla |
| 5.14 | Testing: UI tests para login | ⏸️ Pendiente | 🟢 BAJA | Compose test |

**Criterios de aceptación Semanas 5-6:**
- ✅ Login/register funciona end-to-end
- ✅ Perfil se edita correctamente
- ✅ Navegación fluida
- ✅ Estado persistido

---

### Semana 7-9: Chat y Mensajería

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| 7.1 | Crear ChatListScreen (Compose) | ⏸️ Pendiente | 🔴 CRÍTICA | LazyColumn |
| 7.2 | Crear ChatListItem component | ⏸️ Pendiente | 🔴 CRÍTICA | Avatar, name, last message |
| 7.3 | Implementar ChatListViewModel | ⏸️ Pendiente | 🔴 CRÍTICA | Flow de chats |
| 7.4 | Implementar pull-to-refresh | ⏸️ Pendiente | 🟢 MEDIA | Sincronizar chats |
| 7.5 | Crear ChatDetailScreen (Compose) | ⏸️ Pendiente | 🔴 CRÍTICA | Messages + input |
| 7.6 | Crear MessageBubble component | ⏸️ Pendiente | 🔴 CRÍTICA | Sent/received styling |
| 7.7 | Implementar MessageViewModel | ⏸️ Pendiente | 🔴 CRÍTICA | Paginación de mensajes |
| 7.8 | Implementar MessageInputBar | ⏸️ Pendiente | 🔴 CRÍTICA | TextField + send button |
| 7.9 | Implementar envío de mensaje texto | ⏸️ Pendiente | 🔴 CRÍTICA | End-to-end |
| 7.10 | Implementar recepción en tiempo real (SignalR) | ⏸️ Pendiente | 🔴 CRÍTICA | ReceiveMessage event |
| 7.11 | Implementar status indicators | ⏸️ Pendiente | 🟡 ALTA | Check, double-check, blue |
| 7.12 | Implementar typing indicators | ⏸️ Pendiente | 🟡 ALTA | "Escribiendo..." |
| 7.13 | Implementar online status | ⏸️ Pendiente | 🟡 ALTA | "En línea" / "Última vez" |
| 7.14 | Implementar MessageStatusUpdated (SignalR) | ⏸️ Pendiente | 🟡 ALTA | Actualizar checks |
| 7.15 | Implementar lazy loading de mensajes | ⏸️ Pendiente | 🟡 ALTA | Scroll infinito |
| 7.16 | Implementar scroll to bottom | ⏸️ Pendiente | 🟢 MEDIA | Al enviar mensaje |
| 7.17 | Implementar long press menu | ⏸️ Pendiente | 🟢 MEDIA | Delete, copy, reply |
| 7.18 | Implementar reply to message | ⏸️ Pendiente | 🟢 MEDIA | Con preview |
| 7.19 | Implementar delete message | ⏸️ Pendiente | 🟢 MEDIA | Para mí / para todos |
| 7.20 | Implementar edit message | ⏸️ Pendiente | 🟢 BAJA | "Editado" label |
| 7.21 | Implementar offline queue | ⏸️ Pendiente | 🟡 ALTA | Enviar al reconectar |
| 7.22 | Testing: mensajería end-to-end | ⏸️ Pendiente | 🟡 ALTA | Con 2 usuarios |

**Criterios de aceptación Semanas 7-9:**
- ✅ Chat 1-1 funciona completamente
- ✅ Mensajes en tiempo real
- ✅ Status indicators correctos
- ✅ Typing indicators
- ✅ Offline mode básico

---

### Semana 10-11: Media Support

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| 10.1 | Implementar attach menu (Compose) | ⏸️ Pendiente | 🔴 CRÍTICA | BottomSheet |
| 10.2 | Implementar image picker (Android) | ⏸️ Pendiente | 🔴 CRÍTICA | PickVisualMedia |
| 10.3 | Implementar camera capture (Android) | ⏸️ Pendiente | 🟡 ALTA | CameraX |
| 10.4 | Implementar permisos de cámara | ⏸️ Pendiente | 🟡 ALTA | Accompanist Permissions |
| 10.5 | Crear ImagePreviewScreen | ⏸️ Pendiente | 🔴 CRÍTICA | Antes de enviar |
| 10.6 | Implementar image upload API | ⏸️ Pendiente | 🔴 CRÍTICA | Multipart form |
| 10.7 | Implementar envío de imagen | ⏸️ Pendiente | 🔴 CRÍTICA | Con caption |
| 10.8 | Implementar ImageMessage bubble | ⏸️ Pendiente | 🔴 CRÍTICA | AsyncImage (Coil) |
| 10.9 | Implementar image viewer (fullscreen) | ⏸️ Pendiente | 🟡 ALTA | Zoom, pan |
| 10.10 | Implementar video picker (Android) | ⏸️ Pendiente | 🟡 ALTA | Media selector |
| 10.11 | Implementar video upload API | ⏸️ Pendiente | 🟡 ALTA | Hasta 50MB |
| 10.12 | Implementar VideoMessage bubble | ⏸️ Pendiente | 🟡 ALTA | Thumbnail + play |
| 10.13 | Implementar video player | ⏸️ Pendiente | 🟡 ALTA | ExoPlayer |
| 10.14 | Implementar audio recorder (Android) | ⏸️ Pendiente | 🟡 ALTA | MediaRecorder |
| 10.15 | Crear AudioRecorderUI | ⏸️ Pendiente | 🟡 ALTA | Hold to record |
| 10.16 | Implementar audio upload API | ⏸️ Pendiente | 🟡 ALTA | M4A format |
| 10.17 | Implementar AudioMessage bubble | ⏸️ Pendiente | 🟡 ALTA | Waveform + duration |
| 10.18 | Implementar audio player | ⏸️ Pendiente | 🟡 ALTA | MediaPlayer |
| 10.19 | Implementar document picker | ⏸️ Pendiente | 🟢 MEDIA | PDF, DOCX, etc. |
| 10.20 | Implementar DocumentMessage bubble | ⏸️ Pendiente | 🟢 MEDIA | Icon + filename + size |
| 10.21 | Implementar progress indicators | ⏸️ Pendiente | 🟡 ALTA | Upload/download |
| 10.22 | Implementar image caching (Coil) | ⏸️ Pendiente | 🟢 MEDIA | Disk cache |
| 10.23 | Testing: envío de cada tipo de media | ⏸️ Pendiente | 🟡 ALTA | Image, video, audio, doc |

**Criterios de aceptación Semanas 10-11:**
- ✅ Todos los tipos de media funcionan
- ✅ Upload con progress
- ✅ Preview antes de enviar
- ✅ Reproductor de audio/video

---

### Semana 12: Push Notifications

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| 12.1 | Setup Firebase (google-services.json) | ⏸️ Pendiente | 🔴 CRÍTICA | Firebase Console |
| 12.2 | Implementar FCMService (Android) | ⏸️ Pendiente | 🔴 CRÍTICA | onMessageReceived |
| 12.3 | Implementar obtención de token FCM | ⏸️ Pendiente | 🔴 CRÍTICA | onNewToken |
| 12.4 | Implementar registro de token en backend | ⏸️ Pendiente | 🔴 CRÍTICA | /api/push/subscribe |
| 12.5 | Crear notification channels (Android 8+) | ⏸️ Pendiente | 🔴 CRÍTICA | Messages, Calls |
| 12.6 | Implementar notification para mensaje | ⏸️ Pendiente | 🔴 CRÍTICA | Con avatar, reply action |
| 12.7 | Implementar notification para llamada | ⏸️ Pendiente | 🟡 ALTA | Fullscreen intent |
| 12.8 | Implementar notification para status | ⏸️ Pendiente | 🟢 MEDIA | Opcional |
| 12.9 | Implementar deep linking | ⏸️ Pendiente | 🟡 ALTA | Abrir chat desde notif |
| 12.10 | Implementar reply desde notificación | ⏸️ Pendiente | 🟢 MEDIA | RemoteInput |
| 12.11 | Implementar mark as read desde notif | ⏸️ Pendiente | 🟢 MEDIA | PendingIntent |
| 12.12 | Implementar badge count | ⏸️ Pendiente | 🟢 BAJA | Unread messages |
| 12.13 | Implementar notification settings | ⏸️ Pendiente | 🟢 MEDIA | Mute, custom sounds |
| 12.14 | Testing: recibir push en background | ⏸️ Pendiente | 🟡 ALTA | App cerrada |

**Criterios de aceptación Semana 12:**
- ✅ Push notifications funcionan
- ✅ Deep linking correcto
- ✅ Canales configurados
- ✅ Reply desde notificación

---

## Fase 3: Features Avanzadas (6 semanas)

### Semana 13-14: Stories/Status

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| 13.1 | Crear StatusListScreen (Compose) | ⏸️ Pendiente | 🟡 ALTA | Con anillos de progreso |
| 13.2 | Crear StatusRing component | ⏸️ Pendiente | 🟡 ALTA | Animado |
| 13.3 | Implementar StatusViewModel | ⏸️ Pendiente | 🟡 ALTA | Fetch statuses |
| 13.4 | Implementar create status flow | ⏸️ Pendiente | 🟡 ALTA | Image/video/text |
| 13.5 | Crear StatusCameraScreen | ⏸️ Pendiente | 🟡 ALTA | Capture for status |
| 13.6 | Implementar text status creation | ⏸️ Pendiente | 🟡 ALTA | Backgrounds, fonts |
| 13.7 | Implementar status upload API | ⏸️ Pendiente | 🟡 ALTA | /api/estados |
| 13.8 | Crear StatusViewerScreen | ⏸️ Pendiente | 🟡 ALTA | Stories-like |
| 13.9 | Implementar auto-advance timer | ⏸️ Pendiente | 🟡 ALTA | 5 segundos |
| 13.10 | Implementar progress bars | ⏸️ Pendiente | 🟡 ALTA | Para múltiples status |
| 13.11 | Implementar tap to skip | ⏸️ Pendiente | 🟡 ALTA | Left/right |
| 13.12 | Implementar view tracking | ⏸️ Pendiente | 🟢 MEDIA | Quién vio |
| 13.13 | Implementar status expiration (24h) | ⏸️ Pendiente | 🟡 ALTA | Background cleanup |
| 13.14 | Implementar status privacy settings | ⏸️ Pendiente | 🟢 MEDIA | Todos/contactos/excepto |
| 13.15 | Testing: crear y ver status | ⏸️ Pendiente | 🟡 ALTA | End-to-end |

**Criterios de aceptación Semanas 13-14:**
- ✅ Status creation funciona
- ✅ Viewer stories-like
- ✅ Expiration automática
- ✅ View tracking

---

### Semana 15-17: Voice/Video Calls (WebRTC)

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| 15.1 | **POC: WebRTC Android** | ⏸️ Pendiente | 🔴 CRÍTICA | Validar viabilidad |
| 15.2 | Implementar WebRTCManager (Android) | ⏸️ Pendiente | 🔴 CRÍTICA | PeerConnection setup |
| 15.3 | Configurar STUN servers | ⏸️ Pendiente | 🔴 CRÍTICA | Google STUN |
| 15.4 | Implementar audio/video constraints | ⏸️ Pendiente | 🔴 CRÍTICA | MediaConstraints |
| 15.5 | Implementar createOffer | ⏸️ Pendiente | 🔴 CRÍTICA | SDP generation |
| 15.6 | Implementar createAnswer | ⏸️ Pendiente | 🔴 CRÍTICA | Responder llamada |
| 15.7 | Implementar ICE candidate exchange | ⏸️ Pendiente | 🔴 CRÍTICA | Via SignalR |
| 15.8 | Implementar CallViewModel | ⏸️ Pendiente | 🔴 CRÍTICA | Call state machine |
| 15.9 | Crear IncomingCallScreen (fullscreen) | ⏸️ Pendiente | 🔴 CRÍTICA | Accept/reject |
| 15.10 | Crear ActiveCallScreen | ⏸️ Pendiente | 🔴 CRÍTICA | Video/audio controls |
| 15.11 | Implementar local video view | ⏸️ Pendiente | 🔴 CRÍTICA | SurfaceViewRenderer |
| 15.12 | Implementar remote video view | ⏸️ Pendiente | 🔴 CRÍTICA | SurfaceViewRenderer |
| 15.13 | Implementar toggle audio button | ⏸️ Pendiente | 🟡 ALTA | Mute/unmute |
| 15.14 | Implementar toggle video button | ⏸️ Pendiente | 🟡 ALTA | Camera on/off |
| 15.15 | Implementar switch camera | ⏸️ Pendiente | 🟡 ALTA | Front/back |
| 15.16 | Implementar hang up button | ⏸️ Pendiente | 🔴 CRÍTICA | Close peer connection |
| 15.17 | Implementar call initiation | ⏸️ Pendiente | 🔴 CRÍTICA | CallUser SignalR event |
| 15.18 | Implementar call rejection | ⏸️ Pendiente | 🔴 CRÍTICA | RejectCall event |
| 15.19 | Implementar call end notification | ⏸️ Pendiente | 🔴 CRÍTICA | CallEnded event |
| 15.20 | Crear CallHistoryScreen | ⏸️ Pendiente | 🟢 MEDIA | Llamadas recientes |
| 15.21 | Implementar call logging | ⏸️ Pendiente | 🟢 MEDIA | Guardar en DB |
| 15.22 | Implementar ForegroundService para llamada | ⏸️ Pendiente | 🔴 CRÍTICA | Android 8+ |
| 15.23 | Implementar permisos (CAMERA, RECORD_AUDIO) | ⏸️ Pendiente | 🔴 CRÍTICA | Runtime permissions |
| 15.24 | Implementar proximity sensor | ⏸️ Pendiente | 🟢 MEDIA | Apagar pantalla |
| 15.25 | Implementar audio routing | ⏸️ Pendiente | 🟡 ALTA | Speaker/earpiece |
| 15.26 | Testing: llamada de voz end-to-end | ⏸️ Pendiente | 🔴 CRÍTICA | 2 dispositivos |
| 15.27 | Testing: videollamada end-to-end | ⏸️ Pendiente | 🔴 CRÍTICA | 2 dispositivos |

**Criterios de aceptación Semanas 15-17:**
- ✅ WebRTC POC exitoso
- ✅ Llamada de voz funciona
- ✅ Videollamada funciona
- ✅ Controls funcionan
- ✅ Foreground service activo

---

### Semana 18: Group Chats

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| 18.1 | Crear NewGroupScreen | ⏸️ Pendiente | 🟡 ALTA | Seleccionar contactos |
| 18.2 | Implementar CreateGroupUseCase | ⏸️ Pendiente | 🟡 ALTA | /api/chats/group |
| 18.3 | Implementar GroupInfoScreen | ⏸️ Pendiente | 🟡 ALTA | Editar nombre, imagen |
| 18.4 | Implementar add participant | ⏸️ Pendiente | 🟡 ALTA | Solo admins |
| 18.5 | Implementar remove participant | ⏸️ Pendiente | 🟡 ALTA | Solo admins |
| 18.6 | Implementar leave group | ⏸️ Pendiente | 🟡 ALTA | Para usuarios |
| 18.7 | Implementar promote to admin | ⏸️ Pendiente | 🟢 MEDIA | Cambiar rol |
| 18.8 | Implementar group messages | ⏸️ Pendiente | 🟡 ALTA | Con sender name |
| 18.9 | Implementar ParticipantAdded (SignalR) | ⏸️ Pendiente | 🟡 ALTA | Notificación |
| 18.10 | Implementar ParticipantRemoved (SignalR) | ⏸️ Pendiente | 🟡 ALTA | Notificación |
| 18.11 | Implementar GroupUpdated (SignalR) | ⏸️ Pendiente | 🟡 ALTA | Nombre/imagen |
| 18.12 | Implementar system messages | ⏸️ Pendiente | 🟢 MEDIA | "X se unió", "Y salió" |
| 18.13 | Testing: crear grupo y enviar mensaje | ⏸️ Pendiente | 🟡 ALTA | 3+ usuarios |

**Criterios de aceptación Semana 18:**
- ✅ Grupos se crean correctamente
- ✅ Mensajes grupales funcionan
- ✅ Gestión de participantes
- ✅ Eventos en tiempo real

---

## Fase 4: Polish y Optimización (4 semanas)

### Semana 19-20: Performance y UX

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| 19.1 | Implementar image caching avanzado | ⏸️ Pendiente | 🟡 ALTA | Coil disk/memory cache |
| 19.2 | Implementar pagination de mensajes | ⏸️ Pendiente | 🟡 ALTA | Cargar bajo demanda |
| 19.3 | Implementar lazy loading de chats | ⏸️ Pendiente | 🟡 ALTA | Virtual scrolling |
| 19.4 | Optimizar queries de database | ⏸️ Pendiente | 🟡 ALTA | Índices, EXPLAIN |
| 19.5 | Implementar background sync (WorkManager) | ⏸️ Pendiente | 🟡 ALTA | Mensajes pendientes |
| 19.6 | Implementar offline mode robusto | ⏸️ Pendiente | 🟡 ALTA | Queue + retry |
| 19.7 | Implementar message search | ⏸️ Pendiente | 🟢 MEDIA | FTS (Full-text search) |
| 19.8 | Implementar chat search | ⏸️ Pendiente | 🟢 MEDIA | Filtrar lista |
| 19.9 | Implementar swipe actions | ⏸️ Pendiente | 🟢 MEDIA | Archive, delete |
| 19.10 | Implementar chat archive | ⏸️ Pendiente | 🟢 MEDIA | Ocultar chats |
| 19.11 | Implementar mute chat | ⏸️ Pendiente | 🟢 MEDIA | Sin notificaciones |
| 19.12 | Implementar pin chat | ⏸️ Pendiente | 🟢 MEDIA | Al top |
| 19.13 | Implementar block user | ⏸️ Pendiente | 🟢 MEDIA | /api/users/block |
| 19.14 | Profiling con Android Profiler | ⏸️ Pendiente | 🟡 ALTA | CPU, memory, network |
| 19.15 | Optimizar composables | ⏸️ Pendiente | 🟡 ALTA | remember, derivedStateOf |
| 19.16 | Implementar animations | ⏸️ Pendiente | 🟢 MEDIA | Smooth transitions |

**Criterios de aceptación Semanas 19-20:**
- ✅ App fluida (60fps)
- ✅ Bajo consumo de memoria
- ✅ Offline mode robusto
- ✅ Features UX completas

---

### Semana 21: Testing

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| 21.1 | Unit tests: Domain layer | ⏸️ Pendiente | 🟡 ALTA | >80% coverage |
| 21.2 | Unit tests: UseCases | ⏸️ Pendiente | 🟡 ALTA | Mock repositories |
| 21.3 | Unit tests: Repositories | ⏸️ Pendiente | 🟡 ALTA | Fake APIs |
| 21.4 | Integration tests: API calls | ⏸️ Pendiente | 🟢 MEDIA | MockWebServer |
| 21.5 | Integration tests: Database | ⏸️ Pendiente | 🟢 MEDIA | In-memory DB |
| 21.6 | UI tests: Login flow | ⏸️ Pendiente | 🟡 ALTA | Compose test |
| 21.7 | UI tests: Chat flow | ⏸️ Pendiente | 🟡 ALTA | Send message |
| 21.8 | UI tests: Media upload | ⏸️ Pendiente | 🟢 MEDIA | Mock file picker |
| 21.9 | E2E tests: Full user journey | ⏸️ Pendiente | 🟢 MEDIA | Espresso |
| 21.10 | Manual testing: Dispositivos variados | ⏸️ Pendiente | 🔴 CRÍTICA | Android 8-15 |
| 21.11 | Manual testing: Network conditions | ⏸️ Pendiente | 🟡 ALTA | 2G, 3G, 4G, WiFi |
| 21.12 | Manual testing: Llamadas reales | ⏸️ Pendiente | 🔴 CRÍTICA | 2+ dispositivos |
| 21.13 | Bug fixing sprint | ⏸️ Pendiente | 🔴 CRÍTICA | Resolver P0/P1 |

**Criterios de aceptación Semana 21:**
- ✅ Test coverage >70%
- ✅ 0 bugs P0
- ✅ <5 bugs P1
- ✅ App estable en pruebas

---

### Semana 22: Release Preparation

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| 22.1 | Setup ProGuard/R8 rules | ⏸️ Pendiente | 🔴 CRÍTICA | Minify + obfuscate |
| 22.2 | Configurar signing config | ⏸️ Pendiente | 🔴 CRÍTICA | Keystore |
| 22.3 | Build release APK/AAB | ⏸️ Pendiente | 🔴 CRÍTICA | Verificar tamaño |
| 22.4 | Testing de release build | ⏸️ Pendiente | 🔴 CRÍTICA | Smoke tests |
| 22.5 | Crear assets para Play Store | ⏸️ Pendiente | 🔴 CRÍTICA | Screenshots, icon |
| 22.6 | Escribir descripción de app | ⏸️ Pendiente | 🔴 CRÍTICA | ES + EN |
| 22.7 | Setup Play Console | ⏸️ Pendiente | 🔴 CRÍTICA | App listing |
| 22.8 | Upload a internal testing | ⏸️ Pendiente | 🔴 CRÍTICA | Alpha track |
| 22.9 | Beta testing con usuarios reales | ⏸️ Pendiente | 🟡 ALTA | 10-20 usuarios |
| 22.10 | Feedback iteration | ⏸️ Pendiente | 🟡 ALTA | Fix critical issues |
| 22.11 | Setup Crashlytics | ⏸️ Pendiente | 🟡 ALTA | Firebase |
| 22.12 | Setup Analytics | ⏸️ Pendiente | 🟢 MEDIA | Firebase Analytics |
| 22.13 | Privacy policy page | ⏸️ Pendiente | 🔴 CRÍTICA | Legal requirement |
| 22.14 | Terms of service page | ⏸️ Pendiente | 🔴 CRÍTICA | Legal requirement |
| 22.15 | Upload to beta track | ⏸️ Pendiente | 🔴 CRÍTICA | Pre-production |
| 22.16 | Final QA pass | ⏸️ Pendiente | 🔴 CRÍTICA | Checklist completo |
| 22.17 | **RELEASE a PRODUCTION** | ⏸️ Pendiente | 🔴 CRÍTICA | 🎉 |

**Criterios de aceptación Semana 22:**
- ✅ App en Play Store (beta)
- ✅ 0 crashes críticos
- ✅ Beta feedback positivo
- ✅ Documentación completa

---

## Tareas Post-Release

### v1.1 - Mejoras Futuras

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| P.1 | Mensajes efímeros (disappearing) | ⏸️ Pendiente | 🟢 MEDIA | Timer-based |
| P.2 | Encuestas en grupos | ⏸️ Pendiente | 🟢 BAJA | Voting |
| P.3 | Reacciones a mensajes | ⏸️ Pendiente | 🟢 MEDIA | Emoji reactions |
| P.4 | Mensajes de voz en tiempo real | ⏸️ Pendiente | 🟢 BAJA | WhatsApp-style |
| P.5 | Backup en la nube | ⏸️ Pendiente | 🟡 ALTA | Google Drive |
| P.6 | Temas personalizables | ⏸️ Pendiente | 🟢 BAJA | Dark/light variants |
| P.7 | Stickers personalizados | ⏸️ Pendiente | 🟢 MEDIA | Pack management |
| P.8 | Compartir ubicación en tiempo real | ⏸️ Pendiente | 🟢 MEDIA | Live location |
| P.9 | Llamadas grupales | ⏸️ Pendiente | 🟡 ALTA | Multi-party WebRTC |
| P.10 | Web app sincronizada | ⏸️ Pendiente | 🟡 ALTA | QR code pairing |

---

## Tareas iOS (Post-Android)

### iOS App Development (Mes 6-8)

| ID | Tarea | Estado | Prioridad | Notas |
|----|-------|--------|-----------|-------|
| iOS.1 | Setup Xcode project con shared module | ⏸️ Pendiente | 🔴 CRÍTICA | CocoaPods/SPM |
| iOS.2 | Implementar SignalRClient.ios.kt | ⏸️ Pendiente | 🔴 CRÍTICA | Swift wrapper |
| iOS.3 | Implementar WebRTCManager.ios.kt | ⏸️ Pendiente | 🔴 CRÍTICA | WebRTC.framework |
| iOS.4 | SwiftUI: Login/Register | ⏸️ Pendiente | 🔴 CRÍTICA | iOS UI |
| iOS.5 | SwiftUI: Chat list | ⏸️ Pendiente | 🔴 CRÍTICA | iOS UI |
| iOS.6 | SwiftUI: Chat detail | ⏸️ Pendiente | 🔴 CRÍTICA | iOS UI |
| iOS.7 | SwiftUI: Call screens | ⏸️ Pendiente | 🔴 CRÍTICA | iOS UI |
| iOS.8 | APNs integration | ⏸️ Pendiente | 🔴 CRÍTICA | Push notifications |
| iOS.9 | Testing iOS | ⏸️ Pendiente | 🟡 ALTA | XCTest |
| iOS.10 | TestFlight beta | ⏸️ Pendiente | 🔴 CRÍTICA | Beta testing |
| iOS.11 | App Store submission | ⏸️ Pendiente | 🔴 CRÍTICA | Review process |

**Estimación iOS:** 3 meses (reutilizando 65% del código)

---

## Métricas de Progreso

### Resumen por Fase

| Fase | Tareas Totales | Completadas | En Progreso | Pendientes | % Completado |
|------|----------------|-------------|-------------|------------|--------------|
| **Fase 1: Setup** | 43 | 0 | 0 | 43 | 0% |
| **Fase 2: Core** | 63 | 0 | 0 | 63 | 0% |
| **Fase 3: Avanzadas** | 55 | 0 | 0 | 55 | 0% |
| **Fase 4: Polish** | 46 | 0 | 0 | 46 | 0% |
| **TOTAL ANDROID** | **207** | **0** | **0** | **207** | **0%** |
| **iOS (futuro)** | 11 | 0 | 0 | 11 | 0% |

---

## Riesgos y Blockers

### Críticos (Requieren POC)

| Riesgo | Impacto | Probabilidad | Plan de Mitigación | Estado |
|--------|---------|--------------|-------------------|--------|
| SignalR client no funciona en KMP | 🔴 ALTO | 🟡 MEDIA | POC en Semana 1, fallback a polling | ⏸️ Pendiente |
| WebRTC demasiado complejo | 🔴 ALTO | 🟡 MEDIA | POC en Semana 15, usar SDK third-party | ⏸️ Pendiente |
| Performance issues con listas grandes | 🟡 MEDIO | 🟢 BAJA | Pagination, profiling temprano | ⏸️ Pendiente |

---

## Notas de Implementación

### Decisiones Pendientes

1. **SignalR Client:** Wrapper expect/actual vs WebSocket custom protocol
   - **Decisión:** Semana 1 (basada en POC)
   - **Responsable:** Tech Lead

2. **ViewModels:** Shared vs Android-specific
   - **Decisión:** Semana 4
   - **Opción recomendada:** Shared (mayor reusabilidad)

3. **WebRTC Library:** google-webrtc vs Stream SDK
   - **Decisión:** Semana 15 (basada en POC)
   - **Opción recomendada:** google-webrtc (más control)

### Dependencias Críticas

- ✅ Backend ASP.NET Core (ya existe)
- ✅ SignalR Hub (ya existe)
- ⏸️ Firebase project (crear en Semana 12)
- ⏸️ Google Play Console (crear en Semana 22)
- ⏸️ STUN/TURN servers (configurar en Semana 15)

---

## Changelog

### 2026-02-14
- Creación del plan inicial
- 207 tareas definidas para Android
- 4 fases de desarrollo (22 semanas)
- POCs identificados (SignalR, WebRTC)

---

**Última actualización:** 2026-02-14
**Próxima revisión:** Viernes Semana 1 (Post-POC SignalR)
