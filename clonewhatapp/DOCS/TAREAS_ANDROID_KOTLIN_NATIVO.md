# Lista de Tareas - App Android Kotlin Nativo

**Proyecto:** WhatsApp Clone Android
**Plan:** PLAN_ANDROID_KOTLIN_NATIVO.md
**Fecha inicio:** 2026-02-14

**Leyenda:**
- [ ] Pendiente
- [~] En progreso
- [x] Completada
- **Depende de:** tareas que deben completarse antes
- **Paralelo con:** tareas que pueden ejecutarse al mismo tiempo

---

## MAPA DE DEPENDENCIAS Y PARALELISMO

```
FASE 1: Fundamentos
====================

BLOQUE 1 (Semana 1) — Setup base, todo secuencial
  T-001 → T-002 → T-003 → T-004 (cadena obligatoria)
  Luego en PARALELO:
    ├── T-005 (Hilt/DI)
    ├── T-006 (Theme)
    ├── T-007 (Navegacion)
    └── T-008 (Build variants)

BLOQUE 2 (Semana 2) — Auth, dos carriles paralelos
  Depende de: BLOQUE 1 completo
  CARRIL A (Data layer):          CARRIL B (UI layer):
    T-009 (modelos dominio)         T-006 ya listo (theme)
    ├── T-010 (AuthApi)             T-014 (LoginScreen)      ← paralelo
    ├── T-012 (SecureTokenStorage)  T-015 (RegisterScreen)   ← paralelo
    └── T-013 (AuthInterceptor)
  MERGE:
    T-011 (AuthRepository) ← depende de T-010, T-012, T-013
    T-016 (ViewModels) ← depende de T-011, T-014, T-015
    T-017 (SplashScreen) ← depende de T-011
    T-018 (Nav Auth) ← depende de T-016, T-017

BLOQUE 3 (Semanas 3-4) — Chat, tres carriles paralelos
  Depende de: T-018 (auth completo)
  CARRIL A (Domain):     CARRIL B (Data):        CARRIL C (UI):
    T-019 (modelos)        T-020 (Room DB)         T-030 (ChatBubble)
        │                  T-021 (DAOs)            T-032 (MainScreen + BottomNav)
        ├──────────────────T-022 (ChatsApi)        T-055 (AttachmentMenu placeholder)
        │                  T-023 (MessagesApi)
        │                      │
  MERGE:                       │
    T-024 (ChatRepo) ← T-019, T-020, T-021, T-022
    T-025 (MessageRepo) ← T-019, T-021, T-023
        │
    CARRIL D (Screens):                CARRIL E (Screens):
      T-026 (ChatListScreen)             T-028 (ChatWindowScreen)  ← paralelo
      T-027 (ChatListViewModel)          T-029 (ChatWindowViewModel)
          └── depende de T-024               └── depende de T-025, T-030
    T-031 (Paging 3) ← depende de T-029

BLOQUE 4 (Semanas 5-6) — SignalR, dos carriles
  Depende de: T-029 (chat funcional con REST)
  CARRIL A (Infraestructura):     CARRIL B (despues de A):
    T-033 (SignalRManager)          T-035 (ReceiveMessage)
    T-034 (JWT en SignalR)          T-036 (UserTyping)         ← paralelo
        │                           T-037 (MessageStatus)      ← paralelo
        │                           T-038 (UserStatus)         ← paralelo
        │                           T-039 (SendMessage)        ← paralelo
        │                           T-040 (SendTyping)         ← paralelo
        │                           T-041 (MessageDelivered)   ← paralelo
        │                           T-042 (MessageRead)        ← paralelo
  CARRIL C (independiente de listeners):
    T-043 (Foreground Service) ← depende de T-033
    T-044 (Reconexion) ← depende de T-033
    T-045 (Indicador conexion UI) ← depende de T-033


FASE 2: Multimedia
====================

BLOQUE 5 (Semanas 7-8) — Imagenes/Videos, alto paralelismo
  Depende de: FASE 1 completa
  CARRIL A (Captura):           CARRIL B (UI):              CARRIL C (Infra):
    T-047 (CameraX fotos)        T-049 (ImageBubble)         T-054 (Upload multipart)
    T-051 (CameraX video)        T-050 (Viewer zoom)         T-048 (Compresion)
    T-046 (PhotoPicker)          T-052 (VideoBubble)
                                 T-053 (Video player)
  Todo en CARRIL A, B, C es paralelo entre si
  T-055 (AttachmentMenu final) ← depende de T-046, T-047, T-051

BLOQUE 6 (Semanas 9-10) — Audio/Docs, alto paralelismo
  Depende de: T-054 (upload multipart)
  CARRIL A (Audio):              CARRIL B (Documentos):      CARRIL C (Permisos):
    T-056 (AudioRecorder)          T-061 (Selector docs)       T-064 (Permisos)
    T-057 (RecordButton)           T-062 (DocBubble)
    T-059 (AudioPlayer)            T-063 (Descarga archivos)
    T-060 (Waveform)
    T-058 (AudioBubble)
  Todo paralelo entre carriles
  T-065 (MessageInput mejorado) ← depende de T-057, T-055, T-061


FASE 3: Llamadas
====================

BLOQUE 7 (Semanas 11-13) — Voz, secuencia con paralelismo parcial
  Depende de: T-034 (SignalR funcional)
  SECUENCIA BASE:
    T-066 (WebRTC dep) → T-067 (WebRTCManager) → T-068 (llamada saliente)
                                                → T-069 (llamada entrante)  ← paralelo con T-068
                                                → T-070 (ICE candidates)
  EN PARALELO (despues de T-067):
    ├── T-071 (CallService)
    ├── T-072 (ConnectionService)
    ├── T-073 (IncomingCallScreen)     ← paralelo con otras UI
    ├── T-074 (ActiveCallScreen)       ← paralelo
    ├── T-077 (Listeners SignalR)
  DESPUES de screens:
    T-075 (AudioFocus) ← depende de T-074
    T-076 (Proximity sensor) ← depende de T-074

BLOQUE 8 (Semanas 14-15) — Video, paralelismo parcial
  Depende de: BLOQUE 7 completo (llamadas voz funcionan)
  EN PARALELO:
    ├── T-078 (VideoCallScreen)
    ├── T-079 (SurfaceViewRenderer)
    ├── T-080 (Switch camara)
    ├── T-085 (CallViewModel)
    ├── T-084 (CallHistoryScreen)      ← independiente del resto
  SECUENCIAL:
    T-081 (PiP) ← depende de T-078
    T-082 (Toggle video) ← depende de T-078
    T-083 (Calidad de red) ← depende de T-079


FASE 4: Features Avanzadas
============================

BLOQUE 9, 10, 11 — TODOS en paralelo entre si

BLOQUE 9 (Semanas 16-17) — Stories
  Depende de: FASE 1 completa (Room + Retrofit + Auth)
  CARRIL A (Data):              CARRIL B (UI):
    T-086 (StatusApi)             T-088 (StatusListScreen)
    T-087 (StatusRepo)            T-089 (StatusViewerScreen)   ← paralelo
        │                         T-090 (CreateStatusScreen)   ← paralelo
  MERGE:
    T-091 (Expiracion visual) ← depende de T-088
    T-092 (Visto por) ← depende de T-089

BLOQUE 10 (Semana 18) — Offline
  Depende de: T-025 (MessageRepository)
  EN PARALELO:
    ├── T-093 (Cola pendientes)
    ├── T-095 (NetworkCallback)
  SECUENCIAL:
    T-094 (SyncWorker) ← depende de T-093, T-095
    T-096 (Indicador sync UI) ← depende de T-093
    T-097 (Retry backoff) ← depende de T-094
    T-098 (Sync al abrir) ← depende de T-094

BLOQUE 11 (Semana 19) — Push
  Depende de: T-005 (Hilt), T-011 (AuthRepo para token)
  SECUENCIA:
    T-099 (Firebase setup) → T-100 (MessagingService) → T-101 (Registrar token)
  EN PARALELO (despues de T-100):
    ├── T-102 (Canales notificacion)
    ├── T-103 (Notif mensaje)
    ├── T-104 (Respuesta directa)    ← paralelo
    ├── T-105 (Full-screen llamada)  ← paralelo
    ├── T-106 (Agrupacion)           ← paralelo


FASE 5: Polish y Calidad
==========================

BLOQUE 12, 13, 14 — TODOS en paralelo entre si

BLOQUE 12 (Semanas 20-21) — UX, todo paralelo
  Depende de: FASE 1-4 completas
  TODAS paralelas (son mejoras independientes a pantallas existentes):
    T-107, T-108, T-109, T-110, T-111, T-112, T-113, T-114, T-115

BLOQUE 13 (Semana 22) — Seguridad, todo paralelo
  Depende de: T-012 (SecureTokenStorage), T-005 (Hilt)
  TODAS paralelas:
    T-116, T-117, T-118, T-119, T-120

BLOQUE 14 (Semana 23) — Testing, todo paralelo
  Depende de: Features implementadas
  TODAS paralelas:
    T-121, T-122, T-123, T-124, T-125, T-126
  T-127 (Coverage check) ← depende de T-121 a T-126


FASE 6: Extras — todo paralelo entre si
==========================================
  T-128 a T-135 son independientes entre si
```

---

## RESUMEN DE PARALELISMO OPTIMIZADO

### Maximo paralelismo posible por bloque

| Bloque | Tareas | Secuenciales | Paralelas | Ahorro estimado |
|--------|--------|:---:|:---:|:---:|
| 1 - Setup | 8 | 4 (T-001→004) | 4 (T-005,006,007,008) | 3 tareas |
| 2 - Auth | 10 | 5 (cadena critica) | 5 (2 carriles) | 3 tareas |
| 3 - Chat | 14 | 6 (cadena critica) | 8 (3 carriles) | 5 tareas |
| 4 - SignalR | 13 | 3 (T-033→034→listeners) | 10 (listeners paralelos) | 8 tareas |
| 5 - Imagenes | 10 | 2 | 8 (3 carriles) | 6 tareas |
| 6 - Audio | 10 | 2 | 8 (3 carriles) | 6 tareas |
| 7 - Voz | 12 | 5 | 7 (UI + services paralelos) | 5 tareas |
| 8 - Video | 8 | 3 | 5 | 4 tareas |
| 9 - Stories | 7 | 3 | 4 | 3 tareas |
| 10 - Offline | 6 | 4 | 2 | 1 tarea |
| 11 - Push | 8 | 3 | 5 | 4 tareas |
| 12 - UX | 9 | 0 | **9 (todo paralelo)** | 8 tareas |
| 13 - Seguridad | 5 | 0 | **5 (todo paralelo)** | 4 tareas |
| 14 - Testing | 7 | 1 | 6 | 5 tareas |
| **Totales** | **127** | **36** | **91** | **~65 tareas** |

### Bloques que pueden ejecutarse en paralelo entre si

```
Fase 1: Bloques 1→2→3→4 (secuenciales, fundamentos dependen unos de otros)

Fase 2: Bloque 5 ∥ (parcial con Bloque 6 si upload multipart se hace primero)

Fase 3: Bloque 7→8 (video depende de voz)

Fase 4: Bloque 9 ∥ Bloque 10 ∥ Bloque 11  ← LOS TRES EN PARALELO
         (Stories)  (Offline)    (Push)
         Solo dependen de Fase 1, no entre si

Fase 5: Bloque 12 ∥ Bloque 13 ∥ Bloque 14  ← LOS TRES EN PARALELO
         (UX)       (Seguridad)  (Testing)

Fase 6: T-128 a T-135 ← TODOS EN PARALELO
```

### Oportunidades clave de ahorro de tiempo

1. **Fase 4 completa en paralelo** — Stories, Offline y Push no dependen entre si. En vez de 4 semanas secuenciales, se pueden hacer en ~2 semanas con 3 hilos de trabajo.

2. **Fase 5 completa en paralelo** — UX, Seguridad y Testing son independientes. En vez de 4 semanas, se pueden hacer en ~1.5 semanas.

3. **Dentro de SignalR** — los 8 listeners son independientes entre si y se pueden implementar todos en paralelo una vez que SignalRManager esta listo.

4. **Multimedia** — los 3 carriles (captura, UI, infraestructura) no dependen entre si hasta el merge final.

5. **UI y Data layer** — en cada feature, las pantallas Compose y los repositorios/APIs se pueden desarrollar en paralelo si se definen las interfaces primero.

---

## TIMELINE OPTIMIZADO CON PARALELISMO

```
Semana  1: ████ Bloque 1 (Setup)
Semana  2: ████ Bloque 2 (Auth) — 2 carriles paralelos
Semana  3: ████████ Bloque 3 (Chat) — 3 carriles paralelos
Semana  4: ████████ Bloque 3 cont.
Semana  5: ████████ Bloque 4 (SignalR) — listeners en paralelo
Semana  6: ████████ Bloque 4 cont.
           ──── ENTREGABLE: Chat texto en tiempo real ────
Semana  7: ████████████ Bloque 5 (Imagenes) — 3 carriles
Semana  8: ████████████ Bloque 5 cont. + inicio Bloque 6
Semana  9: ████████████ Bloque 6 (Audio/Docs) — 3 carriles
Semana 10: ████████████ Bloque 6 cont.
           ──── ENTREGABLE: Chat multimedia completo ────
Semana 11: ████████ Bloque 7 (Llamadas voz)
Semana 12: ████████ Bloque 7 cont. — UI y services en paralelo
Semana 13: ████████ Bloque 7 cont.
Semana 14: ██████ Bloque 8 (Videollamadas) — 5 tareas paralelas
Semana 15: ██████ Bloque 8 cont.
           ──── ENTREGABLE: Llamadas voz y video ────
Semana 16: ████████████████ Bloque 9 + 10 + 11 EN PARALELO
Semana 17: ████████████████ (Stories ∥ Offline ∥ Push)
           ──── ENTREGABLE: Paridad funcional ────
Semana 18: ████████████████ Bloque 12 + 13 + 14 EN PARALELO
Semana 19: ████████████████ (UX ∥ Seguridad ∥ Testing)
           ──── ENTREGABLE: Production-ready ────
Semana 20+: ████ Fase 6 (Extras opcionales, todo paralelo)
```

**Timeline original:** 23-26 semanas
**Timeline optimizado:** 19-20 semanas (ahorro de ~4-6 semanas)

---

## TAREAS DETALLADAS CON DEPENDENCIAS

## FASE 1: Fundamentos (Semanas 1-6)

### 1.1 Setup del Proyecto (Semana 1)

- [x] **T-001** Crear proyecto Android con Android Studio (Empty Compose Activity)
- [x] **T-002** Configurar estructura multi-modulo — *Depende de: T-001*
- [x] **T-003** Configurar Gradle KTS con Version Catalog — *Depende de: T-002*
- [x] **T-004** Agregar dependencias base — *Depende de: T-003*
- [x] **T-005** Configurar Hilt y modulos DI — *Depende de: T-004* | *Paralelo con: T-006, T-007, T-008*
- [x] **T-006** Crear Theme Material 3 — *Depende de: T-004* | *Paralelo con: T-005, T-007, T-008*
- [x] **T-007** Configurar navegacion Compose — *Depende de: T-004* | *Paralelo con: T-005, T-006, T-008*
- [x] **T-008** Configurar variantes de build — *Depende de: T-004* | *Paralelo con: T-005, T-006, T-007*

### 1.2 Autenticacion (Semana 2)

- [x] **T-009** Crear modelos de dominio Auth — *Depende de: T-005*
- [x] **T-010** Crear AuthApi (Retrofit) — *Depende de: T-009* | *Paralelo con: T-012, T-013, T-014, T-015*
- [x] **T-011** Crear AuthRepository — *Depende de: T-010, T-012, T-013*
- [x] **T-012** Crear SecureTokenStorage — *Depende de: T-005* | *Paralelo con: T-010, T-013, T-014, T-015*
- [x] **T-013** Crear AuthInterceptor — *Depende de: T-012* | *Paralelo con: T-010, T-014, T-015*
- [x] **T-014** Crear LoginScreen — *Depende de: T-006, T-007* | *Paralelo con: T-010, T-012, T-013, T-015*
- [x] **T-015** Crear RegisterScreen — *Depende de: T-006, T-007* | *Paralelo con: T-010, T-012, T-013, T-014*
- [x] **T-016** Crear LoginViewModel y RegisterViewModel — *Depende de: T-011, T-014, T-015*
- [x] **T-017** Crear SplashScreen — *Depende de: T-011*
- [x] **T-018** Configurar flujo navegacion Auth — *Depende de: T-016, T-017*

### 1.3 Chat Basico (Semanas 3-4)

- [x] **T-019** Crear modelos de dominio Chat/Mensaje — *Depende de: T-009* | *Paralelo con: T-020*
- [x] **T-020** Crear Room Database y entities — *Depende de: T-005* | *Paralelo con: T-019*
- [x] **T-021** Crear DAOs con Flows — *Depende de: T-020*
- [x] **T-022** Crear ChatsApi (Retrofit) — *Depende de: T-019, T-013* | *Paralelo con: T-023, T-030, T-032*
- [x] **T-023** Crear MessagesApi (Retrofit) — *Depende de: T-019, T-013* | *Paralelo con: T-022, T-030, T-032*
- [x] **T-024** Crear ChatRepository — *Depende de: T-019, T-021, T-022*
- [x] **T-025** Crear MessageRepository — *Depende de: T-019, T-021, T-023*
- [x] **T-026** Crear ChatListScreen — *Depende de: T-024, T-006* | *Paralelo con: T-028*
- [x] **T-027** Crear ChatListViewModel — *Depende de: T-024* | *Paralelo con: T-029*
- [x] **T-028** Crear ChatWindowScreen — *Depende de: T-025, T-030* | *Paralelo con: T-026*
- [x] **T-029** Crear ChatWindowViewModel — *Depende de: T-025* | *Paralelo con: T-027*
- [x] **T-030** Crear composable ChatBubble — *Depende de: T-006* | *Paralelo con: T-022, T-023, T-032*
- [x] **T-031** Implementar paginacion Paging 3 — *Depende de: T-029*
- [x] **T-032** Crear MainScreen con BottomNavigation — *Depende de: T-007* | *Paralelo con: T-022, T-023, T-030*

### 1.4 SignalR en Tiempo Real (Semanas 5-6)

- [x] **T-033** Crear SignalRManager — *Depende de: T-005, T-012*
- [x] **T-034** Configurar conexion SignalR con JWT — *Depende de: T-033*
- [x] **T-035** Listener ReceiveMessage — *Depende de: T-034, T-021* | *Paralelo con: T-036 a T-042*
- [x] **T-036** Listener UserTyping — *Depende de: T-034* | *Paralelo con: T-035, T-037 a T-042*
- [x] **T-037** Listener MessageStatusUpdated — *Depende de: T-034, T-021* | *Paralelo con: T-035, T-036, T-038 a T-042*
- [x] **T-038** Listener UserStatusChanged — *Depende de: T-034* | *Paralelo con: T-035 a T-037, T-039 a T-042*
- [x] **T-039** Envio SendMessage via SignalR — *Depende de: T-034, T-025* | *Paralelo con: T-035 a T-038, T-040 a T-042*
- [x] **T-040** Envio SendTyping — *Depende de: T-034* | *Paralelo con: T-035 a T-039, T-041, T-042*
- [x] **T-041** Envio MessageDelivered — *Depende de: T-034* | *Paralelo con: T-035 a T-040, T-042*
- [x] **T-042** Envio MessageRead — *Depende de: T-034* | *Paralelo con: T-035 a T-041*
- [x] **T-043** Foreground Service SignalR — *Depende de: T-033* | *Paralelo con: T-034 a T-042, T-044, T-045*
- [x] **T-044** Reconexion backoff exponencial — *Depende de: T-033* | *Paralelo con: T-043, T-045*
- [x] **T-045** Indicador estado conexion UI — *Depende de: T-033, T-006* | *Paralelo con: T-043, T-044*

---

## FASE 2: Multimedia (Semanas 7-10)

### 2.1 Imagenes y Videos (Semanas 7-8)

- [x] **T-046** PhotoPicker API — *Depende de: Fase 1* | *Paralelo con: T-047 a T-054*
- [x] **T-047** CameraX fotos — *Depende de: Fase 1* | *Paralelo con: T-046, T-048 a T-054*
- [x] **T-048** Compresion de imagenes — *Depende de: Fase 1* | *Paralelo con: T-046, T-047, T-049 a T-054*
- [x] **T-049** ImageMessageBubble — *Depende de: T-030* | *Paralelo con: T-046 a T-048, T-050 a T-054*
- [x] **T-050** Pantalla visualizacion imagen — *Depende de: T-049*
- [x] **T-051** Grabacion video CameraX — *Depende de: T-047* | *Paralelo con: T-046, T-048, T-049, T-052 a T-054*
- [x] **T-052** VideoMessageBubble — *Depende de: T-030* | *Paralelo con: T-046 a T-051, T-053, T-054*
- [x] **T-053** Reproductor video inline — *Depende de: Fase 1* | *Paralelo con: T-046 a T-052, T-054*
- [x] **T-054** Upload multipart con progress — *Depende de: T-013* | *Paralelo con: T-046 a T-053*
- [x] **T-055** AttachmentMenu completo — *Depende de: T-046, T-047, T-051, T-061*

### 2.2 Audio y Documentos (Semanas 9-10)

- [x] **T-056** AudioRecorderService — *Depende de: Fase 1* | *Paralelo con: T-059, T-061 a T-064*
- [x] **T-057** RecordButton composable — *Depende de: T-056* | *Paralelo con: T-059, T-061 a T-064*
- [x] **T-058** AudioMessageBubble — *Depende de: T-059, T-060* | *Paralelo con: T-061 a T-064*
- [x] **T-059** AudioPlayerManager — *Depende de: Fase 1* | *Paralelo con: T-056, T-061 a T-064*
- [x] **T-060** Extraccion waveform — *Depende de: T-056* | *Paralelo con: T-059, T-061 a T-064*
- [x] **T-061** Selector documentos — *Depende de: Fase 1* | *Paralelo con: T-056 a T-060, T-063, T-064*
- [x] **T-062** DocumentMessageBubble — *Depende de: T-030* | *Paralelo con: T-056 a T-061, T-063, T-064*
- [x] **T-063** Descarga archivos con progress — *Depende de: T-054* | *Paralelo con: T-056 a T-062, T-064*
- [x] **T-064** Manejo de permisos — *Depende de: Fase 1* | *Paralelo con: T-056 a T-063*
- [x] **T-065** MessageInput mejorado — *Depende de: T-055, T-057, T-061*

---

## FASE 3: Llamadas (Semanas 11-15)

### 3.1 Llamadas de Voz (Semanas 11-13)

- [x] **T-066** Integrar Stream WebRTC — *Depende de: T-004*
- [x] **T-067** Crear WebRTCManager — *Depende de: T-066*
- [x] **T-068** Flujo llamada saliente — *Depende de: T-067, T-034* | *Paralelo con: T-069*
- [x] **T-069** Flujo llamada entrante — *Depende de: T-067, T-034* | *Paralelo con: T-068*
- [x] **T-070** Intercambio ICE candidates — *Depende de: T-068, T-069*
- [x] **T-071** CallService (Foreground) — *Depende de: T-067* | *Paralelo con: T-072 a T-074, T-077*
- [x] **T-072** ConnectionService Android — *Depende de: T-067* | *Paralelo con: T-071, T-073, T-074, T-077*
- [x] **T-073** IncomingCallScreen — *Depende de: T-006* | *Paralelo con: T-071, T-072, T-074, T-077*
- [x] **T-074** ActiveCallScreen (voz) — *Depende de: T-006* | *Paralelo con: T-071 a T-073, T-077*
- [x] **T-075** AudioFocus — *Depende de: T-074*
- [x] **T-076** Proximity sensor — *Depende de: T-074*
- [x] **T-077** Listeners SignalR llamadas — *Depende de: T-034* | *Paralelo con: T-071 a T-074*

### 3.2 Videollamadas (Semanas 14-15)

- [x] **T-078** VideoCallScreen — *Depende de: T-074* | *Paralelo con: T-079, T-084, T-085*
- [x] **T-079** SurfaceViewRenderer — *Depende de: T-067* | *Paralelo con: T-078, T-084, T-085*
- [x] **T-080** Switch camara — *Depende de: T-079*
- [x] **T-081** Picture-in-Picture — *Depende de: T-078*
- [x] **T-082** Toggle video on/off — *Depende de: T-078*
- [x] **T-083** Deteccion calidad red — *Depende de: T-079*
- [x] **T-084** CallHistoryScreen — *Depende de: T-006, T-005* | *Paralelo con: T-078, T-079, T-085*
- [x] **T-085** CallViewModel — *Depende de: T-067* | *Paralelo con: T-078, T-079, T-084*

---

## FASE 4: Features Avanzadas (Semanas 16-17) — LOS 3 BLOQUES EN PARALELO

### 4.1 Estados/Stories — *Paralelo con: 4.2, 4.3*

- [x] **T-086** StatusApi — *Depende de: T-013* | *Paralelo con: T-088 a T-090*
- [x] **T-087** StatusRepository — *Depende de: T-086, T-021*
- [x] **T-088** StatusListScreen — *Depende de: T-087* | *Paralelo con: T-089, T-090*
- [x] **T-089** StatusViewerScreen — *Depende de: T-087* | *Paralelo con: T-088, T-090*
- [x] **T-090** CreateStatusScreen — *Depende de: T-087* | *Paralelo con: T-088, T-089*
- [x] **T-091** Expiracion visual — *Depende de: T-088*
- [x] **T-092** Lista "visto por" — *Depende de: T-089*

### 4.2 Offline y Sincronizacion — *Paralelo con: 4.1, 4.3*

- [x] **T-093** Cola mensajes pendientes — *Depende de: T-025, T-021* | *Paralelo con: T-095*
- [x] **T-094** SyncWorker (WorkManager) — *Depende de: T-093, T-095*
- [x] **T-095** NetworkCallback — *Depende de: T-005* | *Paralelo con: T-093*
- [x] **T-096** Indicador visual sync — *Depende de: T-093*
- [x] **T-097** Retry backoff exponencial — *Depende de: T-094*
- [x] **T-098** Sync al abrir app — *Depende de: T-094*

### 4.3 Push Notifications — *Paralelo con: 4.1, 4.2*

- [x] **T-099** Configurar Firebase — *Depende de: T-004*
- [x] **T-100** MyFirebaseMessagingService — *Depende de: T-099*
- [x] **T-101** Registrar FCM token — *Depende de: T-100, T-011*
- [x] **T-102** Canales de notificacion — *Depende de: T-100* | *Paralelo con: T-103 a T-106*
- [x] **T-103** Notificacion mensaje — *Depende de: T-100* | *Paralelo con: T-102, T-104 a T-106*
- [x] **T-104** Respuesta directa — *Depende de: T-103*
- [x] **T-105** Full-screen intent llamadas — *Depende de: T-100* | *Paralelo con: T-102, T-103, T-106*
- [x] **T-106** Agrupacion notificaciones — *Depende de: T-103*

---

## FASE 5: Polish y Calidad (Semanas 18-19) — LOS 3 BLOQUES EN PARALELO

### 5.1 UX y Animaciones — *Paralelo con: 5.2, 5.3*

- [x] **T-107** Swipe-to-reply — *Depende de: T-030* | *Paralelo con: T-108 a T-115*
- [x] **T-108** Long-press menu — *Depende de: T-030* | *Paralelo con: T-107, T-109 a T-115*
- [x] **T-109** TypingIndicator animado — *Depende de: T-036* | *Paralelo con: T-107, T-108, T-110 a T-115*
- [x] **T-110** Animaciones transicion — *Depende de: T-007* | *Paralelo con: T-107 a T-109, T-111 a T-115*
- [x] **T-111** Tema oscuro completo — *Depende de: T-006* | *Paralelo con: T-107 a T-110, T-112 a T-115*
- [x] **T-112** Dynamic Colors — *Depende de: T-006* | *Paralelo con: T-107 a T-111, T-113 a T-115*
- [x] **T-113** Edge-to-edge — *Depende de: T-006* | *Paralelo con: T-107 a T-112, T-114, T-115*
- [x] **T-114** Predictive back — *Depende de: T-007* | *Paralelo con: T-107 a T-113, T-115*
- [x] **T-115** Haptic feedback — *Depende de: Fase 1* | *Paralelo con: T-107 a T-114*

### 5.2 Seguridad — *Paralelo con: 5.1, 5.3*

- [x] **T-116** Verificar EncryptedSharedPrefs — *Depende de: T-012* | *Paralelo con: T-117 a T-120*
- [x] **T-117** Certificate Pinning — *Depende de: T-005* | *Paralelo con: T-116, T-118 a T-120*
- [x] **T-118** Autenticacion biometrica — *Depende de: T-005* | *Paralelo con: T-116, T-117, T-119, T-120*
- [x] **T-119** R8/ProGuard config — *Depende de: T-008* | *Paralelo con: T-116 a T-118, T-120*
- [x] **T-120** Auditar permisos — *Depende de: T-064* | *Paralelo con: T-116 a T-119*

### 5.3 Testing — *Paralelo con: 5.1, 5.2*

- [x] **T-121** Unit tests ViewModels — *Depende de: T-016, T-027, T-029* | *Paralelo con: T-122 a T-126*
- [x] **T-122** Unit tests Use Cases/Repos — *Depende de: T-024, T-025* | *Paralelo con: T-121, T-123 a T-126*
- [x] **T-123** Unit tests mappers — *Depende de: T-019, T-020* | *Paralelo con: T-121, T-122, T-124 a T-126*
- [x] **T-124** UI tests Compose — *Depende de: T-014, T-026, T-028* | *Paralelo con: T-121 a T-123, T-125, T-126*
- [x] **T-125** Integration tests Room — *Depende de: T-021* | *Paralelo con: T-121 a T-124, T-126*
- [x] **T-126** Screenshot tests Paparazzi — *Depende de: T-030, T-049, T-058* | *Paralelo con: T-121 a T-125*
- [x] **T-127** Verificar coverage >70% — *Depende de: T-121 a T-126*

---

## FASE 6: Extras Opcionales — TODO EN PARALELO

- [x] **T-128** Bubble notifications — *Depende de: T-103* | *Paralelo con: T-129 a T-135*
- [x] **T-129** Widget chats recientes — *Depende de: T-024* | *Paralelo con: T-128, T-130 a T-135*
- [x] **T-130** App shortcuts dinamicos — *Depende de: T-024* | *Paralelo con: T-128, T-129, T-131 a T-135*
- [x] **T-131** Busqueda de mensajes — *Depende de: T-025, T-021* | *Paralelo con: T-128 a T-130, T-132 a T-135*
- [x] **T-132** Mensajes fijados — *Depende de: T-025* | *Paralelo con: T-128 a T-131, T-133 a T-135*
- [x] **T-133** Reacciones emojis — *Depende de: T-030* | *Paralelo con: T-128 a T-132, T-134, T-135*
- [x] **T-134** Compartir ubicacion — *Depende de: T-054* | *Paralelo con: T-128 a T-133, T-135*
- [x] **T-135** Perfil usuario — *Depende de: T-011, T-006* | *Paralelo con: T-128 a T-134*

---

## CADENA CRITICA (ruta mas larga)

La ruta critica que determina el timeline minimo es:

```
T-001 → T-002 → T-003 → T-004 → T-005 → T-009 → T-010 → T-011 → T-016 → T-018
→ T-019 → T-023 → T-025 → T-029 → T-031
→ T-033 → T-034 → T-035
→ T-054 → T-065
→ T-066 → T-067 → T-068 → T-070
→ T-078 → T-081
→ (Fase 4+5 en paralelo)

Total cadena critica: ~40 tareas secuenciales de 135
```

Las otras 95 tareas se distribuyen en paralelo alrededor de esta cadena.
