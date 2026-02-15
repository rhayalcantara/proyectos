# Equipo de Desarrollo y Asignacion de Tareas

**Proyecto:** WhatsApp Clone Android
**Metodologia:** Scrum (sprints de 1 semana)
**Timeline:** 19-20 semanas

---

## Composicion del Equipo

### DEV-1: Lead Android / Arquitecto (Senior)
**Perfil:** 5+ anos Android, experiencia con Clean Architecture, Hilt, Coroutines, SignalR, WebRTC
**Responsabilidad principal:** Infraestructura, networking, real-time, llamadas
**Foco:** Todo lo que toca red, SignalR, WebRTC, seguridad, arquitectura base

### DEV-2: Android UI / Features (Mid-Senior)
**Perfil:** 3+ anos Android, fuerte en Jetpack Compose, animaciones, UX
**Responsabilidad principal:** Pantallas, componentes UI, tema, animaciones, multimedia UI
**Foco:** Todo lo visual, componentes Compose, navegacion, tema, gestos

### DEV-3: Android Fullstack / Features (Mid)
**Perfil:** 2+ anos Android, conoce Room, WorkManager, Firebase, testing
**Responsabilidad principal:** Persistencia, offline, push notifications, testing, features secundarias
**Foco:** Room, DAOs, WorkManager, FCM, documentos, permisos, tests

### QA-1: QA Engineer
**Perfil:** 2+ anos QA mobile, Espresso, pruebas manuales, reporte de bugs
**Responsabilidad principal:** Plan de pruebas, testing manual, regresion, reporte
**Foco:** Verificacion de cada entregable, pruebas de edge cases, performance

---

## Asignacion Semana por Semana

---

### SEMANA 1: Setup del Proyecto

**Trabajo conjunto (los 3 devs):**
- T-001 a T-004 son secuenciales → **DEV-1** lidera, los demas revisan

**Luego en paralelo:**

| DEV-1 | DEV-2 | DEV-3 |
|-------|-------|-------|
| T-001 Crear proyecto | — (apoya) | — (apoya) |
| T-002 Estructura multi-modulo | — | — |
| T-003 Gradle KTS + Version Catalog | — | — |
| T-004 Dependencias base | — | — |
| T-005 Hilt y modulos DI | T-006 Theme Material 3 | T-008 Variantes de build |
| — | T-007 Navegacion Compose | — |

**QA-1:** Preparar plan de pruebas para autenticacion, configurar dispositivos de prueba

---

### SEMANA 2: Autenticacion

| DEV-1 (Data layer) | DEV-2 (UI layer) | DEV-3 (Seguridad + merge) |
|-----|-----|-----|
| T-009 Modelos dominio Auth | T-014 LoginScreen | T-012 SecureTokenStorage |
| T-010 AuthApi (Retrofit) | T-015 RegisterScreen | T-013 AuthInterceptor |
| T-011 AuthRepository | T-017 SplashScreen | T-016 ViewModels (Login + Register) |
| — | — | T-018 Flujo navegacion Auth |

**QA-1:** Probar flujo login/registro contra backend real, verificar manejo de errores

---

### SEMANA 3: Chat — Domain + Data + UI base

| DEV-1 (Data layer) | DEV-2 (UI layer) | DEV-3 (Database) |
|-----|-----|-----|
| T-019 Modelos dominio Chat/Mensaje | T-030 ChatBubble composable | T-020 Room Database + entities |
| T-022 ChatsApi (Retrofit) | T-032 MainScreen + BottomNav | T-021 DAOs con Flows |
| T-023 MessagesApi (Retrofit) | — | — |

**QA-1:** Probar API de chats y mensajes manualmente, documentar respuestas del backend

---

### SEMANA 4: Chat — Repositories + Screens + Paging

| DEV-1 (Repositories) | DEV-2 (Screens) | DEV-3 (Screens) |
|-----|-----|-----|
| T-024 ChatRepository | T-026 ChatListScreen | T-028 ChatWindowScreen |
| T-025 MessageRepository | T-027 ChatListViewModel | T-029 ChatWindowViewModel |
| — | — | T-031 Paginacion Paging 3 |

**QA-1:** Pruebas E2E: login → ver chats → abrir chat → enviar mensaje → verificar en web

---

### SEMANA 5: SignalR — Infraestructura + Listeners

| DEV-1 (Infraestructura SignalR) | DEV-2 (Listeners UI) | DEV-3 (Listeners Data + Services) |
|-----|-----|-----|
| T-033 SignalRManager | T-036 Listener UserTyping | T-035 Listener ReceiveMessage |
| T-034 Conexion JWT | T-038 Listener UserStatusChanged | T-037 Listener MessageStatusUpdated |
| T-044 Reconexion backoff | T-045 Indicador conexion UI | T-041 Envio MessageDelivered |
| — | — | T-042 Envio MessageRead |

**QA-1:** Probar conexion SignalR, reconexion al perder red, mensajes en tiempo real

---

### SEMANA 6: SignalR — Envios + Foreground Service

| DEV-1 (Services) | DEV-2 (Integracion UI) | DEV-3 (Envios) |
|-----|-----|-----|
| T-043 Foreground Service SignalR | Integrar typing en ChatWindow | T-039 Envio SendMessage via SignalR |
| — | Integrar status en ChatList | T-040 Envio SendTyping |
| — | Pulir indicadores en UI | — |

**QA-1:** Pruebas completas Fase 1: chat texto real-time, typing, status, reconexion
**ENTREGABLE: Chat de texto en tiempo real funcional**

---

### SEMANA 7: Multimedia — Imagenes y Video (inicio)

| DEV-1 (Infraestructura) | DEV-2 (UI multimedia) | DEV-3 (Captura) |
|-----|-----|-----|
| T-054 Upload multipart + progress | T-049 ImageMessageBubble | T-047 CameraX fotos |
| T-048 Compresion imagenes | T-050 Visualizacion imagen (zoom) | T-046 PhotoPicker API |
| — | T-052 VideoMessageBubble | — |

**QA-1:** Probar subida de imagenes al backend, verificar formatos, tamanos limites

---

### SEMANA 8: Multimedia — Video + AttachmentMenu

| DEV-1 (Infra cont.) | DEV-2 (UI cont.) | DEV-3 (Video captura) |
|-----|-----|-----|
| Pulir upload, manejar errores | T-053 Reproductor video inline | T-051 Grabacion video CameraX |
| — | T-055 AttachmentMenu completo | T-064 Manejo de permisos |

**QA-1:** Probar captura foto/video, compresion, preview, reproduccion

---

### SEMANA 9: Audio y Documentos

| DEV-1 (Audio core) | DEV-2 (Audio UI) | DEV-3 (Documentos) |
|-----|-----|-----|
| T-056 AudioRecorderService | T-057 RecordButton (hold/slide) | T-061 Selector documentos |
| T-059 AudioPlayerManager | T-060 Extraccion waveform | T-062 DocumentMessageBubble |
| — | T-058 AudioMessageBubble | T-063 Descarga archivos + progress |

**QA-1:** Probar grabacion audio, reproduccion, formatos de documentos

---

### SEMANA 10: Integracion Multimedia

| DEV-1 | DEV-2 | DEV-3 |
|-----|-----|-----|
| Pulir uploads/downloads | T-065 MessageInput mejorado | Pulir permisos y edge cases |
| Bug fixes multimedia | Integrar todo en ChatWindow | Bug fixes documentos |

**QA-1:** Regresion completa Fase 2: todos los tipos de mensaje, upload/download, permisos
**ENTREGABLE: Chat con multimedia completo**

---

### SEMANA 11: Llamadas de Voz — Setup WebRTC

| DEV-1 (WebRTC core) | DEV-2 (UI llamadas) | DEV-3 (Services) |
|-----|-----|-----|
| T-066 Integrar Stream WebRTC | T-073 IncomingCallScreen | T-077 Listeners SignalR llamadas |
| T-067 WebRTCManager | T-074 ActiveCallScreen (voz) | T-071 CallService (Foreground) |

**QA-1:** Preparar escenarios de prueba para llamadas (misma red, diferente red, 3G/4G)

---

### SEMANA 12: Llamadas de Voz — Flujos de llamada

| DEV-1 (Flujos) | DEV-2 (UI + UX) | DEV-3 (Services) |
|-----|-----|-----|
| T-068 Flujo llamada saliente | T-075 AudioFocus | T-072 ConnectionService Android |
| T-069 Flujo llamada entrante | T-076 Proximity sensor | — |
| T-070 Intercambio ICE candidates | — | — |

**QA-1:** Probar llamadas: saliente, entrante, rechazar, colgar, reconexion

---

### SEMANA 13: Llamadas — Estabilizacion + Inicio Video

| DEV-1 | DEV-2 | DEV-3 |
|-----|-----|-----|
| Bug fixes WebRTC voz | T-078 VideoCallScreen | T-085 CallViewModel |
| T-079 SurfaceViewRenderer | T-084 CallHistoryScreen | — |

**QA-1:** Pruebas intensivas de llamadas de voz, edge cases (llamada durante llamada, etc.)

---

### SEMANA 14: Videollamadas

| DEV-1 (WebRTC video) | DEV-2 (UI video) | DEV-3 (Features video) |
|-----|-----|-----|
| T-083 Deteccion calidad red | T-081 Picture-in-Picture | T-080 Switch camara |
| — | T-082 Toggle video on/off | — |

**QA-1:** Probar videollamada: calidad, PiP, switch camara, toggle video

---

### SEMANA 15: Estabilizacion Llamadas

| DEV-1 | DEV-2 | DEV-3 |
|-----|-----|-----|
| Bug fixes WebRTC | Pulir UI llamadas | Pulir CallHistory |
| Optimizar calidad audio/video | Animaciones transicion | — |

**QA-1:** Regresion completa Fase 3: todos los escenarios de llamada voz + video
**ENTREGABLE: Llamadas de voz y video funcionales**

---

### SEMANAS 16-17: Features Avanzadas — 3 BLOQUES EN PARALELO

Cada dev toma un bloque completo independiente:

| DEV-1: Offline/Sync (Bloque 10) | DEV-2: Stories (Bloque 9) | DEV-3: Push (Bloque 11) |
|-----|-----|-----|
| **Sem 16:** | **Sem 16:** | **Sem 16:** |
| T-093 Cola mensajes pendientes | T-086 StatusApi | T-099 Configurar Firebase |
| T-095 NetworkCallback | T-087 StatusRepository | T-100 FirebaseMessagingService |
| T-094 SyncWorker (WorkManager) | T-088 StatusListScreen | T-101 Registrar FCM token |
| **Sem 17:** | **Sem 17:** | **Sem 17:** |
| T-096 Indicador visual sync | T-089 StatusViewerScreen | T-102 Canales notificacion |
| T-097 Retry backoff | T-090 CreateStatusScreen | T-103 Notificacion mensaje |
| T-098 Sync al abrir app | T-091 Expiracion visual | T-104 Respuesta directa |
| — | T-092 Lista "visto por" | T-105 Full-screen intent llamadas |
| — | — | T-106 Agrupacion notificaciones |

**QA-1 Sem 16:** Probar stories y sync offline en paralelo
**QA-1 Sem 17:** Probar push notifications, regresion stories + offline
**ENTREGABLE: Paridad funcional completa**

---

### SEMANAS 18-19: Polish y Calidad — 3 BLOQUES EN PARALELO

| DEV-1: Seguridad (Bloque 13) | DEV-2: UX (Bloque 12) | DEV-3: Testing (Bloque 14) |
|-----|-----|-----|
| **Sem 18:** | **Sem 18:** | **Sem 18:** |
| T-116 Verificar EncryptedSharedPrefs | T-107 Swipe-to-reply | T-121 Unit tests ViewModels |
| T-117 Certificate Pinning | T-108 Long-press menu | T-122 Unit tests Use Cases/Repos |
| T-118 Autenticacion biometrica | T-109 TypingIndicator animado | T-123 Unit tests mappers |
| **Sem 19:** | **Sem 19:** | **Sem 19:** |
| T-119 R8/ProGuard config | T-110 Animaciones transicion | T-124 UI tests Compose |
| T-120 Auditar permisos | T-111 Tema oscuro completo | T-125 Integration tests Room |
| — | T-112 Dynamic Colors | T-126 Screenshot tests Paparazzi |
| — | T-113 Edge-to-edge | T-127 Verificar coverage >70% |
| — | T-114 Predictive back | — |
| — | T-115 Haptic feedback | — |

**QA-1 Sem 18:** Regresion completa de seguridad, probar biometrics en varios dispositivos
**QA-1 Sem 19:** Regresion final completa, pruebas de performance, reporte final
**ENTREGABLE: App production-ready**

---

### SEMANA 20+ (Opcional): Extras

| DEV-1 | DEV-2 | DEV-3 |
|-----|-----|-----|
| T-128 Bubble notifications | T-129 Widget chats (Glance) | T-131 Busqueda de mensajes |
| T-134 Compartir ubicacion | T-130 App shortcuts | T-132 Mensajes fijados |
| — | T-133 Reacciones emojis | T-135 Perfil usuario |

**QA-1:** Pruebas de extras, prueba final de release, checklist Play Store

---

## Resumen de Carga por Miembro

### DEV-1 (Lead / Arquitecto) — 47 tareas

| Area | Tareas | % del total |
|------|--------|-------------|
| Setup + Arquitectura | T-001 a T-005, T-009 a T-011 | 8 |
| Networking (Retrofit) | T-010, T-022, T-023, T-054 | 4 |
| SignalR | T-033, T-034, T-043, T-044 | 4 |
| WebRTC | T-066 a T-070, T-079, T-083 | 7 |
| Offline/Sync | T-093 a T-098 | 6 |
| Seguridad | T-116 a T-120 | 5 |
| Audio core | T-056, T-059 | 2 |
| Multimedia infra | T-048, T-054 | 2 |
| Extras | T-128, T-134 | 2 |
| Bug fixes + code review | (continuo) | 7 |

### DEV-2 (UI / Features) — 48 tareas

| Area | Tareas | % del total |
|------|--------|-------------|
| Theme + Navegacion | T-006, T-007 | 2 |
| Auth UI | T-014, T-015, T-017 | 3 |
| Chat UI | T-026, T-027, T-030, T-032 | 4 |
| SignalR UI | T-036, T-038, T-045 | 3 |
| Multimedia UI | T-049 a T-053, T-055, T-057, T-058, T-060, T-065 | 10 |
| Llamadas UI | T-073 a T-076, T-078, T-081, T-082 | 7 |
| Stories | T-086 a T-092 | 7 |
| UX/Animaciones | T-107 a T-115 | 9 |
| Extras | T-129, T-130, T-133 | 3 |

### DEV-3 (Fullstack / Features) — 40 tareas

| Area | Tareas | % del total |
|------|--------|-------------|
| Build + Storage | T-008, T-012, T-013 | 3 |
| Auth logic | T-016, T-018 | 2 |
| Database | T-020, T-021 | 2 |
| Chat logic | T-028, T-029, T-031 | 3 |
| SignalR data | T-035, T-037, T-039 a T-042 | 6 |
| Captura media | T-046, T-047, T-051, T-064 | 4 |
| Documentos | T-061 a T-063 | 3 |
| Llamadas | T-071, T-072, T-077, T-080, T-084, T-085 | 6 |
| Push | T-099 a T-106 | 8 |
| Testing | T-121 a T-127 | 7 |
| Extras | T-131, T-132, T-135 | 3 |

### QA-1 (QA Engineer) — Continuo

| Semana | Foco |
|--------|------|
| 1 | Plan de pruebas, setup dispositivos |
| 2 | Auth: login, registro, errores |
| 3-4 | Chat: CRUD, mensajes, paginacion |
| 5-6 | SignalR: real-time, reconexion, typing |
| 7-8 | Multimedia: fotos, videos, compresion |
| 9-10 | Audio, documentos, permisos |
| 11-13 | Llamadas voz: flujos, edge cases |
| 14-15 | Videollamadas: PiP, switch camara |
| 16-17 | Stories + Offline + Push (paralelo) |
| 18-19 | Regresion final, performance, seguridad |
| 20+ | Extras, checklist Play Store |

---

## Distribucion Visual por Semana

```
         DEV-1 (Arquitecto)    DEV-2 (UI)             DEV-3 (Fullstack)      QA-1
Sem  1:  Setup proyecto        Theme + Nav            Build variants         Plan pruebas
Sem  2:  Auth API + Repo       Login/Register UI      Token + Interceptor    Test auth
Sem  3:  APIs Retrofit         ChatBubble + MainScr   Room DB + DAOs         Test APIs
Sem  4:  Repositories          ChatList Screen        ChatWindow Screen      Test E2E chat
Sem  5:  SignalR Manager       Listeners UI           Listeners Data         Test real-time
Sem  6:  Foreground Service    Integrar en UI         Envios SignalR         Regresion F1
         ─── ENTREGABLE: Chat texto real-time ───
Sem  7:  Upload + Compresion   Image/Video Bubbles    CameraX + PhotoPicker  Test upload
Sem  8:  Pulir uploads         Video player + Menu    Video capture + Perms  Test multimedia
Sem  9:  Audio Recorder/Player Audio UI + Waveform    Documentos             Test audio/docs
Sem 10:  Bug fixes             MessageInput final     Bug fixes              Regresion F2
         ─── ENTREGABLE: Chat multimedia ───
Sem 11:  WebRTC setup          Call Screens UI        Call Services          Prep test calls
Sem 12:  Flujos llamada        AudioFocus+Proximity   ConnectionService      Test llamadas
Sem 13:  Bug fixes + Video     VideoCallScreen        CallVM + History       Test voz intens.
Sem 14:  Calidad de red        PiP + Toggle video     Switch camara          Test video
Sem 15:  Estabilizar WebRTC    Pulir UI llamadas      Pulir history          Regresion F3
         ─── ENTREGABLE: Llamadas voz y video ───
Sem 16:  ██ OFFLINE/SYNC ██    ██ STORIES ██          ██ PUSH NOTIF ██       Test paralelo
Sem 17:  ██ (continua) ██      ██ (continua) ██       ██ (continua) ██       Regresion F4
         ─── ENTREGABLE: Paridad funcional ───
Sem 18:  ██ SEGURIDAD ██       ██ UX/ANIM ██          ██ TESTING ██          Regresion segur.
Sem 19:  ██ (continua) ██      ██ (continua) ██       ██ (continua) ██       Regresion FINAL
         ─── ENTREGABLE: Production-ready ───
Sem 20+: Bubbles + Ubicacion   Widget + Shortcuts     Busqueda + Perfil      Checklist Store
```

---

## Reglas del Equipo

1. **Code review:** Toda PR necesita aprobacion de al menos 1 dev (DEV-1 revisa a DEV-2 y DEV-3, y viceversa)
2. **Daily standup:** 15 min cada manana — que hice ayer, que hare hoy, bloqueos
3. **Branch strategy:** `feature/T-XXX-descripcion` → PR a `develop` → merge a `main` al final de cada fase
4. **Definicion de "Done":** Codigo + PR aprobada + QA verifico + sin regresiones
5. **Escalacion:** Si una tarea se bloquea > 4h, escalar en standup
6. **DEV-1 como respaldo:** Si DEV-2 o DEV-3 se atoran, DEV-1 desbloquea y retoma su trabajo despues
