using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using CloneWhatsApp.API.Data;
using CloneWhatsApp.API.DTOs;
using CloneWhatsApp.API.Hubs;
using CloneWhatsApp.API.Models;
using CloneWhatsApp.API.Services;

namespace CloneWhatsApp.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class MessagesController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly IHubContext<ChatHub> _hubContext;
    private readonly IWebHostEnvironment _environment;
    private readonly IPushNotificationService _pushService;

    private readonly string[] _imageExtensions = { ".jpg", ".jpeg", ".png", ".gif", ".webp" };
    private readonly string[] _documentExtensions = { ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".txt", ".zip" };
    private readonly string[] _audioExtensions = { ".mp3", ".wav", ".ogg", ".webm", ".m4a", ".aac" };
    private const long MaxFileSize = 10 * 1024 * 1024; // 10MB

    public MessagesController(
        ApplicationDbContext context,
        IHubContext<ChatHub> hubContext,
        IWebHostEnvironment environment,
        IPushNotificationService pushService)
    {
        _context = context;
        _hubContext = hubContext;
        _environment = environment;
        _pushService = pushService;
    }

    private Guid GetCurrentUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.Parse(userIdClaim!);
    }

    private async Task SendPushToOfflineUsersAsync(Guid chatId, Guid senderId, string senderName, string messageContent, string messageType)
    {
        // Obtener participantes del chat que no son el remitente
        var participantes = await _context.ChatParticipantes
            .Where(cp => cp.ChatId == chatId && cp.UsuarioId != senderId && !cp.Silenciado)
            .Select(cp => cp.UsuarioId)
            .ToListAsync();

        // Filtrar solo los que están offline
        var offlineUsers = participantes
            .Where(uid => !ChatHub.IsUserOnline(uid.ToString()))
            .ToList();

        if (offlineUsers.Any())
        {
            // Preparar el contenido del mensaje según el tipo
            var body = messageType switch
            {
                "Imagen" => "Imagen",
                "Audio" => "Mensaje de voz",
                "Documento" => "Documento",
                "Video" => "Video",
                _ => messageContent?.Length > 100 ? messageContent.Substring(0, 100) + "..." : messageContent ?? "Nuevo mensaje"
            };

            var payload = new PushNotificationPayloadDTO
            {
                Title = senderName,
                Body = body,
                Icon = "/favicon.ico",
                Badge = "/favicon.ico",
                Tag = chatId.ToString(),
                Data = new PushDataDTO
                {
                    ChatId = chatId.ToString(),
                    Type = "message"
                }
            };

            await _pushService.SendNotificationToManyAsync(offlineUsers, payload);
        }
    }

    private async Task<(bool hayBloqueo, bool meBloquearon)> VerificarBloqueoEnChatAsync(Guid chatId, Guid userId)
    {
        // Obtener el chat
        var chat = await _context.Chats
            .Include(c => c.Participantes)
            .FirstOrDefaultAsync(c => c.Id == chatId);

        if (chat == null || chat.Tipo != TipoChat.Grupo)
        {
            // Solo verificar bloqueos en chats individuales
            var otroParticipante = chat?.Participantes
                .FirstOrDefault(p => p.UsuarioId != userId);

            if (otroParticipante != null)
            {
                // Verificar si yo bloqueé al otro
                var yoBloqueando = await _context.UsuariosBloqueados
                    .AnyAsync(ub => ub.UsuarioId == userId && ub.BloqueadoId == otroParticipante.UsuarioId);

                // Verificar si el otro me bloqueó
                var meBloquearon = await _context.UsuariosBloqueados
                    .AnyAsync(ub => ub.UsuarioId == otroParticipante.UsuarioId && ub.BloqueadoId == userId);

                return (yoBloqueando || meBloquearon, meBloquearon);
            }
        }

        return (false, false);
    }

    [HttpGet("chat/{chatId}")]
    public async Task<ActionResult<List<MensajeDTO>>> GetMessages(Guid chatId, [FromQuery] int page = 1, [FromQuery] int pageSize = 50)
    {
        var userId = GetCurrentUserId();

        // Verificar que el usuario es participante del chat
        var isParticipant = await _context.ChatParticipantes
            .AnyAsync(cp => cp.ChatId == chatId && cp.UsuarioId == userId);

        if (!isParticipant)
        {
            return Forbid();
        }

        // Obtener participantes del chat (excluyendo al remitente para calcular estado)
        var participantes = await _context.ChatParticipantes
            .Where(cp => cp.ChatId == chatId)
            .Select(cp => cp.UsuarioId)
            .ToListAsync();

        var mensajes = await _context.Mensajes
            .Include(m => m.Remitente)
            .Include(m => m.MensajeRespondido)
            .Include(m => m.Estados)
            .Where(m => m.ChatId == chatId)
            .OrderByDescending(m => m.FechaEnvio)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .ToListAsync();

        var mensajesDTO = mensajes.Select(m => new MensajeDTO
        {
            Id = m.Id,
            ChatId = m.ChatId,
            RemitenteId = m.RemitenteId,
            RemitenteNombre = m.Remitente.Nombre,
            Contenido = m.EliminadoParaTodos ? "Mensaje eliminado" : m.Contenido,
            Tipo = m.Tipo.ToString(),
            UrlArchivo = m.UrlArchivo,
            NombreArchivo = m.NombreArchivo,
            TamanoArchivo = m.TamanoArchivo,
            DuracionSegundos = m.DuracionSegundos,
            MensajeRespondidoId = m.MensajeRespondidoId,
            MensajeRespondido = m.MensajeRespondido != null ? new MensajeDTO
            {
                Id = m.MensajeRespondido.Id,
                Contenido = m.MensajeRespondido.Contenido,
                RemitenteId = m.MensajeRespondido.RemitenteId
            } : null,
            FechaEnvio = m.FechaEnvio,
            Eliminado = m.Eliminado,
            EliminadoParaTodos = m.EliminadoParaTodos,
            Editado = m.Editado,
            Estado = CalcularEstadoMensaje(m, participantes)
        }).ToList();

        // Ordenar de más antiguo a más reciente para mostrar
        mensajesDTO.Reverse();

        return Ok(mensajesDTO);
    }

    private static string CalcularEstadoMensaje(Mensaje mensaje, List<Guid> participantes)
    {
        // Si no hay estados registrados, está enviado
        if (!mensaje.Estados.Any())
        {
            return "Enviado";
        }

        // Obtener participantes que deben leer (todos menos el remitente)
        var destinatarios = participantes.Where(p => p != mensaje.RemitenteId).ToList();

        if (!destinatarios.Any())
        {
            return "Enviado";
        }

        // Verificar si todos los destinatarios han leído
        var todosLeyeron = destinatarios.All(d =>
            mensaje.Estados.Any(e => e.UsuarioId == d && e.Estado == EstadoMensaje.Leido));

        if (todosLeyeron)
        {
            return "Leido";
        }

        // Verificar si al menos uno ha recibido/entregado
        var algunoEntrego = destinatarios.Any(d =>
            mensaje.Estados.Any(e => e.UsuarioId == d &&
                (e.Estado == EstadoMensaje.Entregado || e.Estado == EstadoMensaje.Leido)));

        if (algunoEntrego)
        {
            return "Entregado";
        }

        return "Enviado";
    }

    [HttpPost]
    public async Task<ActionResult<MensajeDTO>> SendMessage([FromBody] EnviarMensajeDTO dto)
    {
        var userId = GetCurrentUserId();

        // Verificar que el usuario es participante del chat
        var isParticipant = await _context.ChatParticipantes
            .AnyAsync(cp => cp.ChatId == dto.ChatId && cp.UsuarioId == userId);

        if (!isParticipant)
        {
            return Forbid();
        }

        // Verificar bloqueos
        var (hayBloqueo, meBloquearon) = await VerificarBloqueoEnChatAsync(dto.ChatId, userId);
        if (hayBloqueo)
        {
            var errorMsg = meBloquearon
                ? "No puedes enviar mensajes a este usuario porque te ha bloqueado"
                : "No puedes enviar mensajes a un usuario que has bloqueado";
            return BadRequest(new { message = errorMsg, blocked = true });
        }

        var tipoMensaje = Enum.Parse<TipoMensaje>(dto.Tipo);

        var mensaje = new Mensaje
        {
            Id = Guid.NewGuid(),
            ChatId = dto.ChatId,
            RemitenteId = userId,
            Contenido = dto.Contenido,
            Tipo = tipoMensaje,
            MensajeRespondidoId = dto.MensajeRespondidoId,
            FechaEnvio = DateTime.UtcNow
        };

        _context.Mensajes.Add(mensaje);

        // Actualizar última actividad del chat
        var chat = await _context.Chats.FindAsync(dto.ChatId);
        if (chat != null)
        {
            chat.UltimaActividad = DateTime.UtcNow;
        }

        await _context.SaveChangesAsync();

        // Cargar remitente
        var remitente = await _context.Usuarios.FindAsync(userId);

        var mensajeDTO = new MensajeDTO
        {
            Id = mensaje.Id,
            ChatId = mensaje.ChatId,
            RemitenteId = mensaje.RemitenteId,
            RemitenteNombre = remitente?.Nombre,
            Contenido = mensaje.Contenido,
            Tipo = mensaje.Tipo.ToString(),
            FechaEnvio = mensaje.FechaEnvio,
            Estado = "Enviado"
        };

        // Notificar por SignalR
        await _hubContext.Clients.Group(dto.ChatId.ToString())
            .SendAsync("ReceiveMessage", new MensajeEnviadoDTO
            {
                MensajeId = mensaje.Id,
                ChatId = mensaje.ChatId,
                Mensaje = mensajeDTO
            });

        // Enviar push a usuarios offline
        _ = SendPushToOfflineUsersAsync(dto.ChatId, userId, remitente?.Nombre ?? "Usuario", dto.Contenido, dto.Tipo);

        return Ok(mensajeDTO);
    }

    [HttpPost("with-file")]
    [RequestFormLimits(MultipartBodyLengthLimit = 50 * 1024 * 1024)]
    [RequestSizeLimit(50 * 1024 * 1024)]
    public async Task<ActionResult<MensajeDTO>> SendMessageWithFile(
        [FromForm] Guid chatId,
        [FromForm] string? contenido,
        [FromForm] Guid? mensajeRespondidoId,
        [FromForm] int? duracionSegundos,
        IFormFile file)
    {
        var userId = GetCurrentUserId();

        // Verificar que el usuario es participante del chat
        var isParticipant = await _context.ChatParticipantes
            .AnyAsync(cp => cp.ChatId == chatId && cp.UsuarioId == userId);

        if (!isParticipant)
        {
            return Forbid();
        }

        // Verificar bloqueos
        var (hayBloqueo, meBloquearon) = await VerificarBloqueoEnChatAsync(chatId, userId);
        if (hayBloqueo)
        {
            var errorMsg = meBloquearon
                ? "No puedes enviar archivos a este usuario porque te ha bloqueado"
                : "No puedes enviar archivos a un usuario que has bloqueado";
            return BadRequest(new { message = errorMsg, blocked = true });
        }

        if (file == null || file.Length == 0)
        {
            return BadRequest(new { message = "No se ha seleccionado ningún archivo" });
        }

        if (file.Length > MaxFileSize)
        {
            return BadRequest(new { message = "El archivo es demasiado grande. Máximo 10MB" });
        }

        var extension = Path.GetExtension(file.FileName).ToLowerInvariant();
        TipoMensaje tipoMensaje;
        string subFolder;

        if (_imageExtensions.Contains(extension))
        {
            tipoMensaje = TipoMensaje.Imagen;
            subFolder = "messages";
        }
        else if (_audioExtensions.Contains(extension))
        {
            tipoMensaje = TipoMensaje.Audio;
            subFolder = "audio";
        }
        else if (_documentExtensions.Contains(extension))
        {
            tipoMensaje = TipoMensaje.Documento;
            subFolder = "messages";
        }
        else
        {
            return BadRequest(new { message = "Tipo de archivo no permitido" });
        }

        // Guardar archivo
        var fileName = $"{Guid.NewGuid()}{extension}";
        var uploadsFolder = Path.Combine(_environment.WebRootPath, "uploads", subFolder);
        Directory.CreateDirectory(uploadsFolder);
        var filePath = Path.Combine(uploadsFolder, fileName);

        using (var stream = new FileStream(filePath, FileMode.Create))
        {
            await file.CopyToAsync(stream);
        }

        var urlArchivo = $"/uploads/{subFolder}/{fileName}";

        var mensaje = new Mensaje
        {
            Id = Guid.NewGuid(),
            ChatId = chatId,
            RemitenteId = userId,
            Contenido = contenido,
            Tipo = tipoMensaje,
            UrlArchivo = urlArchivo,
            NombreArchivo = file.FileName,
            TamanoArchivo = file.Length,
            DuracionSegundos = tipoMensaje == TipoMensaje.Audio ? duracionSegundos : null,
            MensajeRespondidoId = mensajeRespondidoId,
            FechaEnvio = DateTime.UtcNow
        };

        _context.Mensajes.Add(mensaje);

        // Actualizar última actividad del chat
        var chat = await _context.Chats.FindAsync(chatId);
        if (chat != null)
        {
            chat.UltimaActividad = DateTime.UtcNow;
        }

        await _context.SaveChangesAsync();

        // Cargar remitente
        var remitente = await _context.Usuarios.FindAsync(userId);

        var mensajeDTO = new MensajeDTO
        {
            Id = mensaje.Id,
            ChatId = mensaje.ChatId,
            RemitenteId = mensaje.RemitenteId,
            RemitenteNombre = remitente?.Nombre,
            Contenido = mensaje.Contenido,
            Tipo = mensaje.Tipo.ToString(),
            UrlArchivo = mensaje.UrlArchivo,
            NombreArchivo = mensaje.NombreArchivo,
            TamanoArchivo = mensaje.TamanoArchivo,
            DuracionSegundos = mensaje.DuracionSegundos,
            FechaEnvio = mensaje.FechaEnvio,
            Estado = "Enviado"
        };

        // Notificar por SignalR
        await _hubContext.Clients.Group(chatId.ToString())
            .SendAsync("ReceiveMessage", new MensajeEnviadoDTO
            {
                MensajeId = mensaje.Id,
                ChatId = mensaje.ChatId,
                Mensaje = mensajeDTO
            });

        // Enviar push a usuarios offline
        _ = SendPushToOfflineUsersAsync(chatId, userId, remitente?.Nombre ?? "Usuario", contenido, tipoMensaje.ToString());

        return Ok(mensajeDTO);
    }

    [HttpPut("{id}/read")]
    public async Task<ActionResult> MarkAsRead(Guid id)
    {
        var userId = GetCurrentUserId();

        var mensaje = await _context.Mensajes
            .Include(m => m.Chat)
            .FirstOrDefaultAsync(m => m.Id == id);

        if (mensaje == null)
        {
            return NotFound();
        }

        // Verificar que el usuario es participante
        var isParticipant = await _context.ChatParticipantes
            .AnyAsync(cp => cp.ChatId == mensaje.ChatId && cp.UsuarioId == userId);

        if (!isParticipant)
        {
            return Forbid();
        }

        // Crear o actualizar estado del mensaje
        var estadoExistente = await _context.EstadosMensaje
            .FirstOrDefaultAsync(e => e.MensajeId == id && e.UsuarioId == userId);

        if (estadoExistente == null)
        {
            _context.EstadosMensaje.Add(new EstadoMensajeUsuario
            {
                Id = Guid.NewGuid(),
                MensajeId = id,
                UsuarioId = userId,
                Estado = EstadoMensaje.Leido,
                FechaEstado = DateTime.UtcNow
            });
        }
        else
        {
            estadoExistente.Estado = EstadoMensaje.Leido;
            estadoExistente.FechaEstado = DateTime.UtcNow;
        }

        await _context.SaveChangesAsync();

        // Notificar al remitente
        await _hubContext.Clients.Group(mensaje.ChatId.ToString())
            .SendAsync("MessageStatusUpdated", new
            {
                MessageId = id,
                Status = "Leido",
                UserId = userId
            });

        return Ok();
    }

    [HttpPut("chat/{chatId}/read-all")]
    public async Task<ActionResult> MarkAllAsRead(Guid chatId)
    {
        var userId = GetCurrentUserId();

        // Verificar que el usuario es participante
        var isParticipant = await _context.ChatParticipantes
            .AnyAsync(cp => cp.ChatId == chatId && cp.UsuarioId == userId);

        if (!isParticipant)
        {
            return Forbid();
        }

        // Obtener mensajes no leídos por este usuario (no enviados por él)
        var mensajesNoLeidos = await _context.Mensajes
            .Include(m => m.Estados)
            .Where(m => m.ChatId == chatId && m.RemitenteId != userId)
            .Where(m => !m.Estados.Any(e => e.UsuarioId == userId && e.Estado == EstadoMensaje.Leido))
            .ToListAsync();

        var messageIds = new List<Guid>();

        foreach (var mensaje in mensajesNoLeidos)
        {
            var estadoExistente = mensaje.Estados
                .FirstOrDefault(e => e.UsuarioId == userId);

            if (estadoExistente == null)
            {
                _context.EstadosMensaje.Add(new EstadoMensajeUsuario
                {
                    Id = Guid.NewGuid(),
                    MensajeId = mensaje.Id,
                    UsuarioId = userId,
                    Estado = EstadoMensaje.Leido,
                    FechaEstado = DateTime.UtcNow
                });
            }
            else
            {
                estadoExistente.Estado = EstadoMensaje.Leido;
                estadoExistente.FechaEstado = DateTime.UtcNow;
            }

            messageIds.Add(mensaje.Id);
        }

        await _context.SaveChangesAsync();

        // Notificar a todos en el chat
        if (messageIds.Any())
        {
            await _hubContext.Clients.Group(chatId.ToString())
                .SendAsync("MessagesRead", new
                {
                    ChatId = chatId,
                    MessageIds = messageIds,
                    UserId = userId
                });
        }

        return Ok(new { MarkedAsRead = messageIds.Count });
    }

    [HttpGet("chat/{chatId}/search")]
    public async Task<ActionResult<List<MensajeDTO>>> SearchMessages(
        Guid chatId,
        [FromQuery] string query,
        [FromQuery] int limit = 20)
    {
        var userId = GetCurrentUserId();

        // Verificar que el usuario es participante del chat
        var isParticipant = await _context.ChatParticipantes
            .AnyAsync(cp => cp.ChatId == chatId && cp.UsuarioId == userId);

        if (!isParticipant)
        {
            return Forbid();
        }

        if (string.IsNullOrWhiteSpace(query))
        {
            return Ok(new List<MensajeDTO>());
        }

        var mensajes = await _context.Mensajes
            .Include(m => m.Remitente)
            .Where(m => m.ChatId == chatId &&
                       !m.EliminadoParaTodos &&
                       m.Contenido != null &&
                       m.Contenido.ToLower().Contains(query.ToLower()))
            .OrderByDescending(m => m.FechaEnvio)
            .Take(limit)
            .Select(m => new MensajeDTO
            {
                Id = m.Id,
                ChatId = m.ChatId,
                RemitenteId = m.RemitenteId,
                RemitenteNombre = m.Remitente.Nombre,
                Contenido = m.Contenido,
                Tipo = m.Tipo.ToString(),
                FechaEnvio = m.FechaEnvio
            })
            .ToListAsync();

        return Ok(mensajes);
    }

    [HttpPost("{id}/forward")]
    public async Task<ActionResult<MensajeDTO>> ForwardMessage(Guid id, [FromBody] ForwardMessageDTO dto)
    {
        var userId = GetCurrentUserId();

        // Obtener mensaje original
        var mensajeOriginal = await _context.Mensajes
            .Include(m => m.Remitente)
            .FirstOrDefaultAsync(m => m.Id == id);

        if (mensajeOriginal == null)
        {
            return NotFound();
        }

        // Verificar que el usuario es participante del chat original
        var isParticipantOriginal = await _context.ChatParticipantes
            .AnyAsync(cp => cp.ChatId == mensajeOriginal.ChatId && cp.UsuarioId == userId);

        if (!isParticipantOriginal)
        {
            return Forbid();
        }

        // Verificar que el usuario es participante del chat destino
        var isParticipantTarget = await _context.ChatParticipantes
            .AnyAsync(cp => cp.ChatId == dto.TargetChatId && cp.UsuarioId == userId);

        if (!isParticipantTarget)
        {
            return Forbid();
        }

        // Crear nuevo mensaje
        var nuevoMensaje = new Mensaje
        {
            Id = Guid.NewGuid(),
            ChatId = dto.TargetChatId,
            RemitenteId = userId,
            Contenido = mensajeOriginal.Contenido,
            Tipo = mensajeOriginal.Tipo,
            UrlArchivo = mensajeOriginal.UrlArchivo,
            NombreArchivo = mensajeOriginal.NombreArchivo,
            TamanoArchivo = mensajeOriginal.TamanoArchivo,
            DuracionSegundos = mensajeOriginal.DuracionSegundos,
            FechaEnvio = DateTime.UtcNow
        };

        _context.Mensajes.Add(nuevoMensaje);

        // Actualizar última actividad del chat destino
        var chatDestino = await _context.Chats.FindAsync(dto.TargetChatId);
        if (chatDestino != null)
        {
            chatDestino.UltimaActividad = DateTime.UtcNow;
        }

        await _context.SaveChangesAsync();

        // Cargar remitente
        var remitente = await _context.Usuarios.FindAsync(userId);

        var mensajeDTO = new MensajeDTO
        {
            Id = nuevoMensaje.Id,
            ChatId = nuevoMensaje.ChatId,
            RemitenteId = nuevoMensaje.RemitenteId,
            RemitenteNombre = remitente?.Nombre,
            Contenido = nuevoMensaje.Contenido,
            Tipo = nuevoMensaje.Tipo.ToString(),
            UrlArchivo = nuevoMensaje.UrlArchivo,
            NombreArchivo = nuevoMensaje.NombreArchivo,
            TamanoArchivo = nuevoMensaje.TamanoArchivo,
            DuracionSegundos = nuevoMensaje.DuracionSegundos,
            FechaEnvio = nuevoMensaje.FechaEnvio,
            Estado = "Enviado"
        };

        // Notificar por SignalR
        await _hubContext.Clients.Group(dto.TargetChatId.ToString())
            .SendAsync("ReceiveMessage", new MensajeEnviadoDTO
            {
                MensajeId = nuevoMensaje.Id,
                ChatId = nuevoMensaje.ChatId,
                Mensaje = mensajeDTO
            });

        return Ok(mensajeDTO);
    }

    [HttpDelete("{id}")]
    public async Task<ActionResult> DeleteMessage(Guid id, [FromQuery] bool forEveryone = false)
    {
        var userId = GetCurrentUserId();

        var mensaje = await _context.Mensajes.FindAsync(id);

        if (mensaje == null)
        {
            return NotFound();
        }

        if (mensaje.RemitenteId != userId)
        {
            return Forbid();
        }

        if (forEveryone)
        {
            mensaje.EliminadoParaTodos = true;
            mensaje.Contenido = null;

            // Notificar a todos
            await _hubContext.Clients.Group(mensaje.ChatId.ToString())
                .SendAsync("MessageDeleted", new
                {
                    MessageId = id,
                    ForEveryone = true
                });
        }
        else
        {
            mensaje.Eliminado = true;
        }

        await _context.SaveChangesAsync();

        return NoContent();
    }
}
