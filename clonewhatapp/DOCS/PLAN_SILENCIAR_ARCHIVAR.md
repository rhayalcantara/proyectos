# Plan: Silenciar y Archivar Chats

## Objetivo
Permitir a los usuarios silenciar notificaciones de chats específicos y archivar conversaciones para ocultarlas de la lista principal.

## Funcionalidades

### Silenciar Chat
- Silenciar notificaciones de un chat por tiempo determinado (8h, 1 semana, siempre)
- Icono de silenciado visible en la lista de chats
- No afecta la recepción de mensajes, solo notificaciones

### Archivar Chat
- Mover chat a sección de "Archivados"
- Chat archivado no aparece en la lista principal
- Se puede acceder a chats archivados desde un enlace
- Nuevo mensaje desarchiva automáticamente el chat (opcional)

## Arquitectura

### Backend

#### Modelo ChatParticipante (actualizar)
```csharp
public class ChatParticipante
{
    // ... campos existentes
    public bool Silenciado { get; set; } = false;
    public DateTime? SilenciadoHasta { get; set; } // null = siempre
    public bool Archivado { get; set; } = false;
}
```

#### Endpoints
- PUT /api/chats/{id}/silenciar - Silenciar/desilenciar chat
- PUT /api/chats/{id}/archivar - Archivar/desarchivar chat
- GET /api/chats/archivados - Obtener chats archivados

### Frontend

#### ChatService
- `muteChat(chatId, duration)` - Silenciar chat
- `unmuteChat(chatId)` - Quitar silencio
- `archiveChat(chatId)` - Archivar chat
- `unarchiveChat(chatId)` - Desarchivar chat
- `archivedChats` signal - Lista de chats archivados

#### UI
- Menú contextual en cada chat (click derecho o botón)
- Opciones: Silenciar, Archivar
- Icono de silenciado en chat silenciado
- Sección "Chats archivados" al final de la lista

## Tareas

### Backend
- [x] B1: Agregar campos Silenciado, SilenciadoHasta, Archivado a ChatParticipante
- [x] B2: Crear migración de base de datos
- [x] B3: Endpoint PUT /api/chats/{id}/silenciar
- [x] B4: Endpoint PUT /api/chats/{id}/archivar
- [x] B5: Modificar GET /api/chats para excluir archivados
- [x] B6: Endpoint GET /api/chats/archivados

### Frontend
- [x] F1: Actualizar modelo Chat con campos silenciado/archivado
- [x] F2: Métodos en ChatService para silenciar/archivar
- [x] F3: Menú contextual en chat-list-item
- [x] F4: Icono de silenciado en chat-list
- [x] F5: Sección "Chats archivados" en chat-list
- [x] F6: Modal de opciones de silencio (duración)

## Estimación
- Backend: 4 tareas
- Frontend: 6 tareas
- Total: 10 tareas
