# Resumen de Progreso - Clon de WhatsApp

**Fecha**: 2025-12-30
**Estado**: Fase 5.3 En Progreso - Notificaciones Push Completadas

---

## Tecnologías Utilizadas

| Componente | Tecnología | Versión |
|------------|------------|---------|
| Backend | ASP.NET Core Web API | .NET 9 |
| Base de Datos | MySQL | 9.5.0 |
| ORM | Entity Framework Core | 9.0.0 |
| Real-time | SignalR | Incluido en .NET 9 |
| Frontend | Angular | 19 |
| Autenticación | JWT Bearer | - |

---

## Estructura del Proyecto

```
clonewhatapp/
├── DOCS/
│   ├── PLAN_WHATSAPP.md      # Arquitectura y diseño
│   ├── TAREAS.md             # Lista de tareas detallada
│   └── PROGRESO.md           # Este archivo
├── CloneWhatsApp.API/        # Backend C#
│   ├── Controllers/          # API REST endpoints
│   ├── Models/               # Entidades de base de datos
│   ├── DTOs/                 # Data Transfer Objects
│   ├── Services/             # Lógica de negocio
│   ├── Hubs/                 # SignalR para real-time
│   └── Data/                 # DbContext
└── clone-whatsapp-web/       # Frontend Angular
    └── src/app/
        ├── core/             # Servicios, guards, interceptors
        └── features/         # Módulos de la aplicación
            ├── auth/         # Login y registro
            └── chat/         # Chats y mensajes
```

---

## Funcionalidades Completadas

### Backend

| Funcionalidad | Estado | Endpoint |
|---------------|--------|----------|
| Registro de usuario | ✅ | POST /api/auth/register |
| Login con JWT | ✅ | POST /api/auth/login |
| Obtener perfil | ✅ | GET /api/users/me |
| Actualizar perfil | ✅ | PUT /api/users/me |
| Listar contactos | ✅ | GET /api/contacts |
| Agregar contacto | ✅ | POST /api/contacts |
| Eliminar contacto | ✅ | DELETE /api/contacts/{id} |
| Buscar usuarios | ✅ | GET /api/users/search |
| Listar chats | ✅ | GET /api/chats |
| Crear chat individual | ✅ | POST /api/chats/individual |
| Crear grupo | ✅ | POST /api/chats/grupo |
| Actualizar grupo | ✅ | PUT /api/chats/grupo/{id} |
| Imagen del grupo | ✅ | POST /api/chats/grupo/{id}/imagen |
| Agregar participantes | ✅ | POST /api/chats/grupo/{id}/participantes |
| Eliminar participante | ✅ | DELETE /api/chats/grupo/{id}/participantes/{userId} |
| Cambiar rol participante | ✅ | PUT /api/chats/grupo/{id}/participantes/{userId}/rol |
| Salir del grupo | ✅ | POST /api/chats/grupo/{id}/salir |
| Listar participantes | ✅ | GET /api/chats/grupo/{id}/participantes |
| Obtener mensajes | ✅ | GET /api/messages/chat/{id} |
| Enviar mensaje | ✅ | POST /api/messages |
| Enviar mensaje con archivo | ✅ | POST /api/messages/with-file |
| Eliminar mensaje | ✅ | DELETE /api/messages/{id} |
| Marcar como leído | ✅ | PUT /api/messages/{id}/read |
| Marcar todos como leídos | ✅ | PUT /api/messages/chat/{id}/read-all |
| Buscar mensajes | ✅ | GET /api/messages/chat/{id}/search |
| Reenviar mensaje | ✅ | POST /api/messages/{id}/forward |
| Historial llamadas | ✅ | GET /api/calls |
| Iniciar llamada | ✅ | POST /api/calls/start |
| Finalizar llamada | ✅ | PUT /api/calls/{id}/end |

### SignalR Hub (Real-time)

| Evento | Descripción |
|--------|-------------|
| JoinChat | Unirse a sala de chat |
| LeaveChat | Salir de sala de chat |
| SendMessage | Enviar mensaje en tiempo real |
| ReceiveMessage | Recibir mensaje nuevo |
| MessageStatusUpdated | Estado de mensaje actualizado |
| MessagesRead | Mensajes marcados como leídos |
| MessageDeleted | Mensaje eliminado para todos |
| UserTyping | Indicador de escritura |
| UserOnline | Usuario conectado |
| UserOffline | Usuario desconectado |
| ParticipantAdded | Participante agregado a grupo |
| ParticipantRemoved | Participante eliminado de grupo |
| ParticipantLeft | Participante salió del grupo |
| ParticipantRoleChanged | Rol de participante cambiado |
| GroupUpdated | Información del grupo actualizada |
| CallUser | Iniciar llamada WebRTC |
| AnswerCall | Responder llamada |
| RejectCall | Rechazar llamada |
| EndCall | Finalizar llamada |
| SendICECandidate | Enviar candidato ICE |
| IncomingCall | Llamada entrante |
| CallAnswered | Llamada contestada |
| CallRejected | Llamada rechazada |
| CallEnded | Llamada finalizada |
| ICECandidate | Candidato ICE recibido |

### Frontend

| Componente | Estado | Descripción |
|------------|--------|-------------|
| LoginComponent | ✅ | Formulario de inicio de sesión |
| RegisterComponent | ✅ | Formulario de registro |
| ChatLayoutComponent | ✅ | Layout principal (sidebar + chat) |
| ChatListComponent | ✅ | Lista de conversaciones |
| ChatWindowComponent | ✅ | Ventana de chat con mensajes |
| NewChatModalComponent | ✅ | Modal para nuevo chat/contacto |
| AudioPlayerComponent | ✅ | Reproductor de mensajes de voz |
| IncomingCallComponent | ✅ | Modal de llamada entrante |
| ActiveCallComponent | ✅ | Pantalla de llamada activa |

### Servicios Angular

| Servicio | Estado | Función |
|----------|--------|---------|
| AuthService | ✅ | Autenticación y sesión |
| ChatService | ✅ | Gestión de chats y mensajes |
| ContactService | ✅ | Gestión de contactos |
| SignalRService | ✅ | Conexión WebSocket |
| AudioRecorderService | ✅ | Grabación de mensajes de voz |
| WebRTCService | ✅ | Gestión de RTCPeerConnection |
| CallService | ✅ | Orquestación de llamadas |
| ThemeService | ✅ | Gestión de tema claro/oscuro |

---

## Cómo Ejecutar

### Backend
```bash
cd CloneWhatsApp.API
dotnet run
# Servidor en http://localhost:5181
```

### Frontend
```bash
cd clone-whatsapp-web
ng serve
# Aplicación en http://localhost:4200
```

### Base de Datos
- MySQL corriendo en localhost
- Base de datos: `CloneWhatsApp`
- Usuario: `root`
- Password: (vacío)

---

## Usuarios de Prueba

| Nombre | Teléfono | Password |
|--------|----------|----------|
| Rhay Alcantara | +18093038210 | (registrado previamente) |
| Usuario Test | +18091234567 | test123 |

---

## Fase 1 Completada

Todas las tareas de la Fase 1 han sido completadas:
- ✅ Subida de foto de perfil (POST /api/users/me/photo)
- ✅ Eliminación de foto de perfil (DELETE /api/users/me/photo)
- ✅ Componente de perfil completo (panel deslizable)
- ✅ Edición de nombre y estado
- ✅ Responsive design (mobile, tablet, desktop)

---

## Fase 2 Completada

Todas las tareas de la Fase 2 han sido completadas:
- ✅ Estados de mensaje (enviado, entregado, leído) con doble check azul
- ✅ Envío de imágenes y documentos (hasta 10MB)
- ✅ Responder mensajes (cita del mensaje original)
- ✅ Copiar mensajes al portapapeles
- ✅ Eliminar mensajes (para mí / para todos)
- ✅ Búsqueda de mensajes en chat
- ✅ Reenviar mensajes a otros chats

---

## Fase 3 Completada

Todas las tareas de la Fase 3 han sido completadas:

### Backend - Endpoints de Grupos
- ✅ POST /api/chats/grupo - Crear grupo
- ✅ PUT /api/chats/grupo/{id} - Actualizar nombre/descripción
- ✅ POST /api/chats/grupo/{id}/imagen - Subir imagen del grupo
- ✅ POST /api/chats/grupo/{id}/participantes - Agregar participantes
- ✅ DELETE /api/chats/grupo/{id}/participantes/{userId} - Eliminar participante
- ✅ PUT /api/chats/grupo/{id}/participantes/{userId}/rol - Cambiar rol (Admin/Participante)
- ✅ POST /api/chats/grupo/{id}/salir - Salir del grupo
- ✅ GET /api/chats/grupo/{id}/participantes - Listar participantes

### SignalR - Eventos de Grupo
- ✅ ParticipantAdded - Notificar cuando se agrega participante
- ✅ ParticipantRemoved - Notificar cuando se elimina participante
- ✅ ParticipantLeft - Notificar cuando alguien sale del grupo
- ✅ ParticipantRoleChanged - Notificar cambio de rol
- ✅ GroupUpdated - Notificar actualización de grupo

### Frontend - Componentes
- ✅ Pestaña "Nuevo Grupo" en NewChatModal
- ✅ Selección múltiple de contactos para grupo
- ✅ Creación de grupo con nombre y descripción
- ✅ Mostrar nombre del remitente en mensajes de grupo
- ✅ Mensajes de sistema estilizados (entró, salió, creó grupo)

---

## Fase 4 Completada

Todas las tareas de la Fase 4 han sido completadas:

### Backend - Endpoints de Estados
- ✅ POST /api/estados - Publicar estado de texto
- ✅ POST /api/estados/imagen - Publicar estado con imagen
- ✅ GET /api/estados - Obtener mis estados
- ✅ GET /api/estados/contactos - Obtener estados de contactos
- ✅ POST /api/estados/{id}/vista - Marcar estado como visto
- ✅ GET /api/estados/{id}/vistas - Ver quién vio mi estado
- ✅ DELETE /api/estados/{id} - Eliminar mi estado
- ✅ EstadoCleanupService - Limpieza automática de estados expirados (24h)

### SignalR - Eventos de Estados
- ✅ NewStatus - Notificar nuevo estado a contactos
- ✅ StatusViewed - Notificar al creador cuando alguien ve su estado
- ✅ StatusDeleted - Notificar eliminación de estado

### Frontend - Componentes
- ✅ Pestañas Chats/Estados en sidebar
- ✅ StatusListComponent - Lista de estados de contactos
- ✅ CreateStatusComponent - Modal para crear estado (texto/imagen)
- ✅ StatusViewerComponent - Visor carousel de estados
- ✅ StatusService - Servicio Angular para estados
- ✅ Anillo de progreso por estado visto/no visto

---

## Fase 5.1 Completada - Mensajes de Voz

Todas las tareas de Mensajes de Voz han sido completadas:

### Backend
- ✅ Campo `DuracionSegundos` agregado al modelo Mensaje
- ✅ Endpoint actualizado para recibir duración de audio
- ✅ Soporte para archivos de audio (.mp3, .wav, .ogg, .webm, .m4a, .aac)
- ✅ Almacenamiento en `/uploads/audio/`

### Frontend
- ✅ `AudioRecorderService` - Servicio para grabar audio con MediaRecorder API
- ✅ Botón de micrófono en chat-window (cambia a botón enviar cuando hay texto)
- ✅ UI de grabación con indicador de duración y forma de onda animada
- ✅ `AudioPlayerComponent` - Reproductor personalizado con:
  - Play/Pause
  - Barra de progreso clickeable
  - Velocidad 1x, 1.5x, 2x
  - Forma de onda visual estática

---

## Fase 5.2 Completada - Llamadas WebRTC

Todas las tareas de Llamadas WebRTC han sido completadas:

### Backend - Modelo y Endpoints
- ✅ Modelo `Llamada` con enums `TipoLlamada` (Voz/Video) y `EstadoLlamada`
- ✅ GET /api/calls - Historial de llamadas del usuario
- ✅ POST /api/calls/start - Iniciar registro de llamada
- ✅ PUT /api/calls/{id}/end - Finalizar llamada con duración

### SignalR - Eventos de Llamadas
- ✅ `CallUser` - Iniciar llamada con SDP offer
- ✅ `AnswerCall` - Responder llamada con SDP answer
- ✅ `RejectCall` - Rechazar llamada entrante
- ✅ `EndCall` - Finalizar llamada activa
- ✅ `SendICECandidate` - Intercambio de candidatos ICE
- ✅ Eventos: `IncomingCall`, `CallAnswered`, `CallRejected`, `CallEnded`, `ICECandidate`

### Frontend - Servicios
- ✅ `WebRTCService` - Gestión de RTCPeerConnection:
  - Servidores STUN de Google
  - Creación de offer/answer SDP
  - Manejo de candidatos ICE
  - Streams de video local/remoto
  - Control de audio (mute) y video (toggle)
- ✅ `CallService` - Orquestación de llamadas:
  - Estado de llamada con signals
  - Inicio de llamada de voz/video
  - Aceptar/rechazar llamadas entrantes
  - Temporizador de duración
  - Integración con SignalR

### Frontend - Componentes
- ✅ `IncomingCallComponent` - Modal de llamada entrante:
  - Animación de timbre
  - Información del llamador
  - Botones aceptar/rechazar
- ✅ `ActiveCallComponent` - Pantalla de llamada activa:
  - Video local (picture-in-picture)
  - Video remoto a pantalla completa
  - Avatar para llamadas de voz
  - Controles: mute, video, colgar
  - Temporizador de duración
- ✅ Botones de llamada en header de chat individual
- ✅ Overlays de llamada en chat-layout

---

## Fase 5.3 Parcial - Modo Oscuro

El modo oscuro ha sido implementado:

### Implementación
- ✅ Variables CSS globales en `:root` (tema claro) y `[data-theme="dark"]` (tema oscuro)
- ✅ `ThemeService` para gestión de estado del tema
- ✅ Persistencia en localStorage
- ✅ Detección automática de preferencia del sistema (`prefers-color-scheme`)
- ✅ Toggle de tema en header del sidebar (icono sol/luna)
- ✅ Transiciones suaves entre temas

### Componentes Actualizados
- ✅ `styles.scss` - Variables CSS globales
- ✅ `chat-layout.component.scss`
- ✅ `chat-list.component.scss`
- ✅ `chat-window.component.scss`
- ✅ `new-chat-modal.component.scss`
- ✅ `login.component.scss`
- ✅ `register.component.scss`

### Paleta de Colores Oscuros
- Fondo app: `#0b141a`
- Fondo primario: `#111b21`
- Fondo secundario: `#202c33`
- Texto primario: `#e9edef`
- Burbuja propia: `#005c4b`
- Burbuja otro: `#202c33`

---

## Fase 5.3 Parcial - Silenciar/Archivar Chats

La funcionalidad de silenciar y archivar chats ha sido implementada:

### Backend
- ✅ Campos `Silenciado`, `SilenciadoHasta`, `Archivado` en ChatParticipante
- ✅ Migración de base de datos aplicada
- ✅ PUT /api/chats/{id}/silenciar - Silenciar/desilenciar chat
- ✅ PUT /api/chats/{id}/archivar - Archivar/desarchivar chat
- ✅ GET /api/chats/archivados - Obtener chats archivados
- ✅ GET /api/chats modificado para excluir archivados

### Frontend
- ✅ Modelo Chat actualizado con campos silenciado/archivado
- ✅ Métodos en ChatService (muteChat, unmuteChat, archiveChat, unarchiveChat)
- ✅ Menú contextual (click derecho) en cada chat
- ✅ Icono de silenciado visible en la lista de chats
- ✅ Sección "Chats archivados" colapsable
- ✅ Modal de selección de duración de silencio (8h, 1 semana, siempre)

---

## Fase 5.3 Parcial - Bloquear Usuarios

La funcionalidad de bloquear usuarios ha sido implementada:

### Backend
- Modelo `UsuarioBloqueado` con relaciones Usuario/Bloqueado
- Migración de base de datos aplicada
- GET /api/users/blocked - Obtener usuarios bloqueados
- POST /api/users/block/{userId} - Bloquear usuario
- DELETE /api/users/block/{userId} - Desbloquear usuario
- GET /api/users/{userId}/blocked-status - Verificar estado de bloqueo
- Verificación de bloqueos en MessagesController (SendMessage, SendMessageWithFile)
- Verificación de bloqueos en EstadosController (estados de contactos)
- Verificación de bloqueos en CallsController (iniciar llamadas)
- Verificación de bloqueos en ChatHub (llamadas WebRTC)

### Frontend
- BlockService para gestión de bloqueos
- Menú de opciones en header del chat
- Banner de notificación cuando hay bloqueo
- Botón de bloquear/desbloquear en menú del chat
- Deshabilita entrada de mensajes cuando hay bloqueo

---

## Fase 5.3 Parcial - Notificaciones Push

La funcionalidad de notificaciones push ha sido implementada:

### Backend
- Modelo `SuscripcionPush` con campos: Endpoint, P256dh, Auth
- Migración de base de datos aplicada
- Claves VAPID generadas y configuradas en appsettings.json
- GET /api/push/vapid-public-key - Obtener clave pública VAPID
- POST /api/push/subscribe - Registrar suscripción push
- POST /api/push/unsubscribe - Cancelar suscripción
- PushNotificationService para envío de notificaciones
- Integración en MessagesController (mensajes a usuarios offline)
- Integración en ChatHub (llamadas perdidas)

### Frontend
- Service Worker (sw-push.js) para recibir y mostrar notificaciones
- PushNotificationService en Angular
- Solicitud automática de permiso al iniciar sesión
- Click en notificación navega al chat correspondiente

### Características
- Notificaciones cuando el usuario está offline
- Soporte para mensajes de texto, imágenes, audio, documentos
- Notificaciones de llamadas perdidas
- Respeta configuración de silenciar chats
- Auto-limpieza de suscripciones inválidas

---

## Próximas Fases

### Fase 5.3: Características Avanzadas Pendientes
- Cifrado end-to-end

---

## Estadísticas

| Métrica | Valor |
|---------|-------|
| Total de tareas | 115 |
| Tareas completadas | 114 |
| Progreso general | 99% |
| Fase 1 completada | 100% |
| Fase 2 completada | 100% |
| Fase 3 completada | 100% |
| Fase 4 completada | 100% |
| Fase 5.1 completada | 100% |
| Fase 5.2 completada | 100% |
| Fase 5.3 modo oscuro | 100% |
| Fase 5.3 silenciar/archivar | 100% |
| Fase 5.3 bloquear usuarios | 100% |
| Fase 5.3 notificaciones push | 100% |

---

## Notas Técnicas

1. **Base de datos**: Se cambió de SQL Server a MySQL por compatibilidad con macOS
2. **EF Core Provider**: Se usa `MySql.EntityFrameworkCore` (Oracle) en lugar de Pomelo por compatibilidad con EF Core 9
3. **SignalR**: Configurado con autenticación JWT para conexiones WebSocket
4. **CORS**: Configurado para permitir requests desde localhost:4200
