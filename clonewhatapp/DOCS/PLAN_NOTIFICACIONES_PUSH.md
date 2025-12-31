# Plan de Implementación - Notificaciones Push

**Fecha**: 2025-12-30
**Tarea**: 5.3.1 - Implementar notificaciones push

---

## Objetivo

Implementar notificaciones push en el navegador para notificar a los usuarios de:
- Nuevos mensajes cuando no están en la aplicación
- Llamadas entrantes
- Nuevos estados de contactos

---

## Tecnología Seleccionada

**Web Push API + Service Workers**

Ventajas:
- Nativo del navegador, no requiere servicios externos de pago
- Funciona cuando la pestaña está cerrada
- Compatible con Chrome, Firefox, Edge, Safari
- No requiere Firebase (simplifica la implementación)

---

## Arquitectura

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Frontend      │     │    Backend      │     │   Service       │
│   Angular       │────▶│   ASP.NET       │────▶│   Worker        │
│                 │     │                 │     │                 │
│ - Solicita      │     │ - Almacena      │     │ - Recibe push   │
│   permiso       │     │   suscripción   │     │ - Muestra       │
│ - Registra SW   │     │ - Envía push    │     │   notificación  │
│ - Suscribe      │     │   cuando        │     │ - Click abre    │
│                 │     │   usuario       │     │   la app        │
│                 │     │   offline       │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## Tareas de Implementación

### Backend (ASP.NET Core)

#### 1. Modelo de Suscripción Push
- Crear modelo `SuscripcionPush` con:
  - Id (Guid)
  - UsuarioId (FK)
  - Endpoint (string)
  - P256dh (string) - clave pública
  - Auth (string) - secreto de autenticación
  - FechaCreacion
  - Activo (bool)

#### 2. Generar Claves VAPID
- Crear claves VAPID (Voluntary Application Server Identification)
- Almacenar en appsettings.json
- PublicKey se comparte con frontend
- PrivateKey se usa para firmar mensajes

#### 3. Controlador de Suscripciones
- POST /api/push/subscribe - Registrar suscripción
- DELETE /api/push/unsubscribe - Cancelar suscripción
- GET /api/push/vapid-public-key - Obtener clave pública

#### 4. Servicio de Push
- PushNotificationService con método SendAsync
- Usar librería WebPush para .NET
- Integrar en MessagesController y ChatHub

#### 5. Lógica de Envío
- Al recibir mensaje: verificar si destinatario está offline
- Si offline: enviar notificación push
- Para llamadas: siempre enviar push (urgente)

### Frontend (Angular)

#### 6. Service Worker
- Crear sw-push.js para manejar eventos push
- Registrar en app al iniciar
- Manejar evento 'push' para mostrar notificación
- Manejar evento 'notificationclick' para abrir app

#### 7. Servicio de Notificaciones
- PushNotificationService con:
  - requestPermission() - solicitar permiso
  - subscribe() - suscribir a push
  - unsubscribe() - cancelar suscripción
  - isSupported() - verificar soporte

#### 8. Integración en UI
- Solicitar permiso al iniciar sesión
- Toggle en configuración para activar/desactivar
- Indicador de estado de notificaciones

---

## Estructura de Archivos a Crear/Modificar

### Backend (Nuevos)
```
CloneWhatsApp.API/
├── Models/
│   └── SuscripcionPush.cs          # Nuevo modelo
├── DTOs/
│   └── PushDTOs.cs                 # DTOs para push
├── Controllers/
│   └── PushController.cs           # Nuevo controlador
├── Services/
│   └── PushNotificationService.cs  # Nuevo servicio
```

### Backend (Modificar)
```
- Data/ApplicationDbContext.cs      # Agregar DbSet
- Program.cs                        # Registrar servicio
- appsettings.json                  # Claves VAPID
- Controllers/MessagesController.cs # Trigger push
- Hubs/ChatHub.cs                   # Trigger push llamadas
```

### Frontend (Nuevos)
```
clone-whatsapp-web/
├── src/
│   ├── sw-push.js                  # Service Worker
│   └── app/core/services/
│       └── push-notification.service.ts
```

### Frontend (Modificar)
```
- angular.json                      # Registrar SW
- src/main.ts                       # Inicializar SW
- src/app/features/chat/components/chat-layout/
    chat-layout.component.ts        # Solicitar permiso
```

---

## Paquetes NuGet Requeridos

```xml
<PackageReference Include="WebPush" Version="1.0.12" />
```

---

## Flujo de Implementación

1. **Usuario abre la app por primera vez**
   - App solicita permiso de notificaciones
   - Si acepta: registra Service Worker
   - Obtiene suscripción del navegador
   - Envía suscripción al backend

2. **Usuario recibe mensaje estando offline**
   - Mensaje llega a MessagesController
   - Backend detecta que usuario no está conectado a SignalR
   - Backend envía push notification
   - Service Worker muestra notificación

3. **Usuario hace click en notificación**
   - Service Worker captura click
   - Abre/enfoca la app
   - Navega al chat correspondiente

---

## Payload de Notificación

```json
{
  "title": "Nuevo mensaje de Juan",
  "body": "Hola, ¿cómo estás?",
  "icon": "/assets/icons/icon-192x192.png",
  "badge": "/assets/icons/badge-72x72.png",
  "data": {
    "chatId": "guid-del-chat",
    "type": "message"
  },
  "actions": [
    { "action": "open", "title": "Abrir" },
    { "action": "dismiss", "title": "Cerrar" }
  ]
}
```

---

## Consideraciones de Seguridad

- Las claves VAPID privadas nunca se exponen al cliente
- Las suscripciones se asocian a usuarios autenticados
- Se valida que el usuario solo pueda gestionar sus suscripciones
- Los payloads se cifran con las claves del navegador

---

## Orden de Implementación

1. Backend: Modelo y migración
2. Backend: Generar claves VAPID
3. Backend: PushController
4. Backend: PushNotificationService
5. Frontend: Service Worker
6. Frontend: PushNotificationService
7. Frontend: Integración UI
8. Backend: Integrar en MessagesController
9. Backend: Integrar en ChatHub (llamadas)
10. Pruebas end-to-end

---
