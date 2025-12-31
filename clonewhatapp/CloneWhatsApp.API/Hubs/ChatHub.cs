using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using CloneWhatsApp.API.Data;
using CloneWhatsApp.API.DTOs;
using CloneWhatsApp.API.Services;

namespace CloneWhatsApp.API.Hubs;

[Authorize]
public class ChatHub : Hub
{
    private readonly ApplicationDbContext _context;
    private readonly IPushNotificationService _pushService;
    private static readonly Dictionary<string, string> _userConnections = new();

    public ChatHub(ApplicationDbContext context, IPushNotificationService pushService)
    {
        _context = context;
        _pushService = pushService;
    }

    public override async Task OnConnectedAsync()
    {
        var userId = Context.User?.FindFirst(ClaimTypes.NameIdentifier)?.Value;

        if (userId != null)
        {
            _userConnections[userId] = Context.ConnectionId;

            // Marcar usuario como en línea
            var user = await _context.Usuarios.FindAsync(Guid.Parse(userId));
            if (user != null)
            {
                user.EstaEnLinea = true;
                user.UltimaConexion = DateTime.UtcNow;
                await _context.SaveChangesAsync();
            }

            // Unirse a los grupos de sus chats
            var chatIds = await _context.ChatParticipantes
                .Where(cp => cp.UsuarioId == Guid.Parse(userId))
                .Select(cp => cp.ChatId.ToString())
                .ToListAsync();

            foreach (var chatId in chatIds)
            {
                await Groups.AddToGroupAsync(Context.ConnectionId, chatId);
            }

            // Notificar a los contactos que está en línea
            await NotifyContactsStatus(userId, true);
        }

        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        var userId = Context.User?.FindFirst(ClaimTypes.NameIdentifier)?.Value;

        if (userId != null)
        {
            _userConnections.Remove(userId);

            // Marcar usuario como desconectado
            var user = await _context.Usuarios.FindAsync(Guid.Parse(userId));
            if (user != null)
            {
                user.EstaEnLinea = false;
                user.UltimaConexion = DateTime.UtcNow;
                await _context.SaveChangesAsync();
            }

            // Notificar a los contactos que está desconectado
            await NotifyContactsStatus(userId, false);
        }

        await base.OnDisconnectedAsync(exception);
    }

    public async Task JoinChat(string chatId)
    {
        await Groups.AddToGroupAsync(Context.ConnectionId, chatId);
    }

    public async Task LeaveChat(string chatId)
    {
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, chatId);
    }

    public async Task SendMessage(MensajeEnviadoDTO mensaje)
    {
        await Clients.Group(mensaje.ChatId.ToString()).SendAsync("ReceiveMessage", mensaje);
    }

    public async Task SendTyping(TypingDTO typing)
    {
        await Clients.OthersInGroup(typing.ChatId.ToString()).SendAsync("UserTyping", typing);
    }

    public async Task MessageDelivered(Guid messageId, Guid chatId)
    {
        await Clients.Group(chatId.ToString()).SendAsync("MessageStatusUpdated", new
        {
            MessageId = messageId,
            Status = "Entregado"
        });
    }

    public async Task MessageRead(Guid messageId, Guid chatId)
    {
        await Clients.Group(chatId.ToString()).SendAsync("MessageStatusUpdated", new
        {
            MessageId = messageId,
            Status = "Leido"
        });
    }

    // Eventos de grupo
    public async Task NotifyGroupUpdated(Guid chatId, string nombre, string? descripcion, string? imagen)
    {
        await Clients.Group(chatId.ToString()).SendAsync("GroupUpdated", new
        {
            ChatId = chatId,
            Nombre = nombre,
            Descripcion = descripcion,
            Imagen = imagen
        });
    }

    public async Task NotifyParticipantAdded(Guid chatId, ParticipanteDTO participante)
    {
        // Agregar al usuario al grupo de SignalR si está conectado
        var connectionId = GetConnectionId(participante.UsuarioId.ToString());
        if (connectionId != null)
        {
            await Groups.AddToGroupAsync(connectionId, chatId.ToString());
        }

        await Clients.Group(chatId.ToString()).SendAsync("ParticipantAdded", new
        {
            ChatId = chatId,
            Participante = participante
        });
    }

    public async Task NotifyParticipantRemoved(Guid chatId, Guid usuarioId, string nombre)
    {
        // Remover al usuario del grupo de SignalR si está conectado
        var connectionId = GetConnectionId(usuarioId.ToString());
        if (connectionId != null)
        {
            await Groups.RemoveFromGroupAsync(connectionId, chatId.ToString());
        }

        await Clients.Group(chatId.ToString()).SendAsync("ParticipantRemoved", new
        {
            ChatId = chatId,
            UsuarioId = usuarioId,
            Nombre = nombre
        });
    }

    public async Task NotifyParticipantLeft(Guid chatId, Guid usuarioId, string nombre)
    {
        // Remover al usuario del grupo de SignalR
        var connectionId = GetConnectionId(usuarioId.ToString());
        if (connectionId != null)
        {
            await Groups.RemoveFromGroupAsync(connectionId, chatId.ToString());
        }

        await Clients.Group(chatId.ToString()).SendAsync("ParticipantLeft", new
        {
            ChatId = chatId,
            UsuarioId = usuarioId,
            Nombre = nombre
        });
    }

    public async Task NotifyRoleChanged(Guid chatId, Guid usuarioId, string nuevoRol)
    {
        await Clients.Group(chatId.ToString()).SendAsync("ParticipantRoleChanged", new
        {
            ChatId = chatId,
            UsuarioId = usuarioId,
            NuevoRol = nuevoRol
        });
    }

    public static string? GetConnectionId(string userId)
    {
        return _userConnections.TryGetValue(userId, out var connectionId) ? connectionId : null;
    }

    public static bool IsUserOnline(string userId)
    {
        return _userConnections.ContainsKey(userId);
    }

    // ============ LLAMADAS WebRTC ============

    public async Task CallUser(string targetUserId, string callType, string sdpOffer)
    {
        var callerId = Context.User?.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (callerId == null) return;

        var caller = await _context.Usuarios.FindAsync(Guid.Parse(callerId));
        if (caller == null) return;

        var callerGuid = Guid.Parse(callerId);
        var targetGuid = Guid.Parse(targetUserId);

        // Verificar bloqueos
        var yoBloqueando = await _context.UsuariosBloqueados
            .AnyAsync(ub => ub.UsuarioId == callerGuid && ub.BloqueadoId == targetGuid);

        if (yoBloqueando)
        {
            await Clients.Caller.SendAsync("CallFailed", new
            {
                Reason = "Blocked",
                Message = "No puedes llamar a un usuario que has bloqueado"
            });
            return;
        }

        var meBloquearon = await _context.UsuariosBloqueados
            .AnyAsync(ub => ub.UsuarioId == targetGuid && ub.BloqueadoId == callerGuid);

        if (meBloquearon)
        {
            await Clients.Caller.SendAsync("CallFailed", new
            {
                Reason = "Blocked",
                Message = "No puedes llamar a este usuario"
            });
            return;
        }

        var targetConnectionId = GetConnectionId(targetUserId);
        if (targetConnectionId != null)
        {
            await Clients.Client(targetConnectionId).SendAsync("ReceiveCallOffer", new
            {
                CallerId = callerId,
                CallerName = caller.Nombre,
                CallerPhoto = caller.FotoPerfil,
                CallType = callType,
                SdpOffer = sdpOffer
            });
        }
        else
        {
            // Usuario no está en línea - enviar push notification
            var callTypeText = callType == "video" ? "videollamada" : "llamada de voz";
            var payload = new PushNotificationPayloadDTO
            {
                Title = $"Llamada perdida de {caller.Nombre}",
                Body = $"Intentó comunicarse contigo por {callTypeText}",
                Icon = "/favicon.ico",
                Badge = "/favicon.ico",
                Tag = $"call-{callerId}",
                Data = new PushDataDTO
                {
                    Type = "missed_call"
                }
            };

            _ = _pushService.SendNotificationAsync(targetGuid, payload);

            await Clients.Caller.SendAsync("CallFailed", new
            {
                Reason = "UserOffline",
                Message = "El usuario no está disponible"
            });
        }
    }

    public async Task AnswerCall(string callerId, string sdpAnswer)
    {
        var connectionId = GetConnectionId(callerId);
        if (connectionId != null)
        {
            await Clients.Client(connectionId).SendAsync("ReceiveCallAnswer", new
            {
                SdpAnswer = sdpAnswer
            });
        }
    }

    public async Task RejectCall(string callerId, string reason = "Rejected")
    {
        var userId = Context.User?.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        var connectionId = GetConnectionId(callerId);
        if (connectionId != null)
        {
            await Clients.Client(connectionId).SendAsync("CallRejected", new
            {
                UserId = userId,
                Reason = reason
            });
        }
    }

    public async Task EndCall(string otherUserId)
    {
        var connectionId = GetConnectionId(otherUserId);
        if (connectionId != null)
        {
            await Clients.Client(connectionId).SendAsync("CallEnded", new
            {
                EndedBy = Context.User?.FindFirst(ClaimTypes.NameIdentifier)?.Value
            });
        }
    }

    public async Task SendICECandidate(string targetUserId, string candidate)
    {
        var connectionId = GetConnectionId(targetUserId);
        if (connectionId != null)
        {
            await Clients.Client(connectionId).SendAsync("ReceiveICECandidate", new
            {
                Candidate = candidate
            });
        }
    }

    // ============ FIN LLAMADAS ============

    private async Task NotifyContactsStatus(string userId, bool isOnline)
    {
        // Obtener todos los chats del usuario
        var chatIds = await _context.ChatParticipantes
            .Where(cp => cp.UsuarioId == Guid.Parse(userId))
            .Select(cp => cp.ChatId.ToString())
            .ToListAsync();

        foreach (var chatId in chatIds)
        {
            await Clients.OthersInGroup(chatId).SendAsync("UserStatusChanged", new
            {
                UserId = userId,
                IsOnline = isOnline,
                LastSeen = DateTime.UtcNow
            });
        }
    }
}
