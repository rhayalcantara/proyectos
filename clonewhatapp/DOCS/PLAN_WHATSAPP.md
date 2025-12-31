# Plan de Desarrollo - Clon de WhatsApp

## Tecnologías
- **Backend**: C# (.NET 8 / ASP.NET Core)
- **Frontend**: Angular 17+
- **Base de Datos**: SQL Server / PostgreSQL
- **Comunicación en Tiempo Real**: SignalR (WebSockets)
- **Almacenamiento de Archivos**: Azure Blob Storage / Sistema de archivos local

---

## 1. Cómo Funciona WhatsApp - Análisis

### 1.1 Arquitectura General

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Cliente   │────▶│   Servidor  │────▶│  Base de    │
│  (Angular)  │◀────│   (C# API)  │◀────│   Datos     │
└─────────────┘     └─────────────┘     └─────────────┘
       │                   │
       │     WebSocket     │
       
       └───────────────────┘
            (SignalR)
```

### 1.2 Funcionalidades Principales de WhatsApp

#### A. Sistema de Autenticación
- Registro con número de teléfono
- Verificación por código SMS/OTP
- Sesión persistente con tokens JWT
- Cierre de sesión en otros dispositivos

#### B. Gestión de Usuarios
- Perfil de usuario (nombre, foto, estado)
- Lista de contactos
- Bloqueo de usuarios
- Última vez conectado / En línea

#### C. Sistema de Mensajería
- **Mensajes de texto**: Envío y recepción en tiempo real
- **Mensajes multimedia**: Imágenes, videos, audio, documentos
- **Estados de mensaje**:
  - Enviado (✓)
  - Entregado (✓✓)
  - Leído (✓✓ azul)
- **Mensajes de voz**: Grabación y reproducción
- **Responder a mensajes**: Quote/Reply
- **Reenviar mensajes**
- **Eliminar mensajes**: Para mí / Para todos
- **Mensajes destacados**: Marcar favoritos

#### D. Chats Individuales
- Conversación 1 a 1
- Historial de mensajes
- Búsqueda en conversación
- Exportar chat
- Silenciar notificaciones

#### E. Grupos
- Crear grupo con nombre e imagen
- Agregar/eliminar participantes
- Roles: Admin, Participante
- Configuración del grupo (quién puede enviar mensajes)
- Descripción del grupo
- Salir del grupo

#### F. Estados/Stories
- Publicar estado (imagen, video, texto)
- Duración de 24 horas
- Ver quién vio el estado
- Privacidad de estados

#### G. Llamadas (Fase Avanzada)
- Llamadas de voz
- Videollamadas
- Llamadas grupales

#### H. Notificaciones
- Push notifications
- Notificaciones en tiempo real
- Configuración de notificaciones por chat

### 1.3 Flujo de Mensajes

```
1. Usuario A escribe mensaje
2. Cliente envía mensaje al servidor vía WebSocket
3. Servidor guarda mensaje en BD con estado "enviado"
4. Servidor busca conexión activa de Usuario B
5. Si Usuario B está conectado:
   - Servidor envía mensaje vía WebSocket
   - Cliente B recibe y muestra mensaje
   - Cliente B envía confirmación de "entregado"
   - Servidor actualiza estado y notifica a A
6. Si Usuario B NO está conectado:
   - Mensaje queda en cola
   - Cuando B se conecte, recibe mensajes pendientes
7. Cuando B abre el chat:
   - Cliente B envía confirmación de "leído"
   - Servidor actualiza y notifica a A
```

### 1.4 Modelo de Datos Principal

```
Usuario
├── Id
├── NumeroTelefono
├── Nombre
├── FotoPerfil
├── Estado
├── UltimaConexion
└── EstaEnLinea

Contacto
├── UsuarioId
├── ContactoId
├── NombrePersonalizado
└── Bloqueado

Chat
├── Id
├── Tipo (Individual/Grupo)
├── FechaCreacion
└── UltimoMensaje

ChatParticipante
├── ChatId
├── UsuarioId
├── Rol (Admin/Participante)
├── Silenciado
└── FechaUnion

Mensaje
├── Id
├── ChatId
├── RemitenteId
├── Contenido
├── TipoMensaje (Texto/Imagen/Video/Audio/Documento)
├── UrlArchivo
├── MensajeRespondidoId
├── FechaEnvio
├── Eliminado
└── EliminadoParaTodos

EstadoMensaje
├── MensajeId
├── UsuarioId
├── Estado (Enviado/Entregado/Leido)
└── FechaEstado

Grupo
├── ChatId
├── Nombre
├── Descripcion
├── ImagenGrupo
└── ConfiguracionMensajes

EstadoUsuario (Stories)
├── Id
├── UsuarioId
├── Contenido
├── TipoContenido
├── FechaCreacion
└── FechaExpiracion

VistaEstado
├── EstadoId
├── UsuarioId
└── FechaVista
```

---

## 2. Arquitectura del Sistema

### 2.1 Backend (C# - ASP.NET Core)

```
CloneWhatsApp.API/
├── Controllers/
│   ├── AuthController.cs
│   ├── UsersController.cs
│   ├── ChatsController.cs
│   ├── MessagesController.cs
│   ├── GroupsController.cs
│   └── StatusController.cs
├── Hubs/
│   └── ChatHub.cs (SignalR)
├── Services/
│   ├── AuthService.cs
│   ├── UserService.cs
│   ├── ChatService.cs
│   ├── MessageService.cs
│   ├── FileService.cs
│   └── NotificationService.cs
├── Models/
├── DTOs/
├── Data/
│   └── ApplicationDbContext.cs
└── Middleware/
```

### 2.2 Frontend (Angular)

```
clone-whatsapp-web/
├── src/app/
│   ├── core/
│   │   ├── services/
│   │   ├── guards/
│   │   └── interceptors/
│   ├── shared/
│   │   ├── components/
│   │   └── pipes/
│   ├── features/
│   │   ├── auth/
│   │   ├── chat/
│   │   ├── contacts/
│   │   ├── groups/
│   │   ├── profile/
│   │   └── status/
│   └── layouts/
```

---

## 3. Fases de Desarrollo

### Fase 1: Fundamentos (MVP)
- Autenticación básica
- Perfil de usuario
- Lista de contactos
- Chat individual (solo texto)
- Mensajes en tiempo real con SignalR

### Fase 2: Mensajería Completa
- Estados de mensaje (enviado, entregado, leído)
- Mensajes multimedia (imágenes, archivos)
- Responder mensajes
- Eliminar mensajes
- Búsqueda de mensajes

### Fase 3: Grupos
- Crear y administrar grupos
- Roles y permisos
- Funcionalidades de grupo completas

### Fase 4: Estados/Stories
- Publicar estados
- Ver estados de contactos
- Privacidad de estados

### Fase 5: Características Avanzadas
- Mensajes de voz
- Llamadas (WebRTC)
- Videollamadas
- Cifrado end-to-end

---

## 4. Consideraciones Técnicas

### Seguridad
- JWT para autenticación
- HTTPS obligatorio
- Validación de entrada
- Rate limiting
- Sanitización de archivos

### Rendimiento
- Paginación de mensajes
- Caché de datos frecuentes
- Compresión de imágenes
- Lazy loading en frontend

### Escalabilidad
- Arquitectura stateless
- Redis para caché y sesiones SignalR
- Base de datos optimizada con índices

---

**Fecha de creación**: $(date +%Y-%m-%d)
**Estado**: En planificación
