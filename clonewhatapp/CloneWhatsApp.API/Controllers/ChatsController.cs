using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using CloneWhatsApp.API.Data;
using CloneWhatsApp.API.DTOs;
using CloneWhatsApp.API.Models;
using CloneWhatsApp.API.Hubs;

namespace CloneWhatsApp.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class ChatsController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly IHubContext<ChatHub> _hubContext;

    public ChatsController(ApplicationDbContext context, IHubContext<ChatHub> hubContext)
    {
        _context = context;
        _hubContext = hubContext;
    }

    private Guid GetCurrentUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.Parse(userIdClaim!);
    }

    private async Task<Mensaje> CreateSystemMessage(Guid chatId, Guid userId, string contenido)
    {
        var mensaje = new Mensaje
        {
            Id = Guid.NewGuid(),
            ChatId = chatId,
            RemitenteId = userId,
            Contenido = contenido,
            Tipo = TipoMensaje.Sistema,
            FechaEnvio = DateTime.UtcNow
        };

        _context.Mensajes.Add(mensaje);

        var chat = await _context.Chats.FindAsync(chatId);
        if (chat != null)
            chat.UltimaActividad = DateTime.UtcNow;

        return mensaje;
    }

    [HttpGet]
    public async Task<ActionResult<List<ChatDTO>>> GetChats()
    {
        var userId = GetCurrentUserId();

        var chats = await _context.ChatParticipantes
            .Include(cp => cp.Chat)
                .ThenInclude(c => c.Participantes)
                    .ThenInclude(p => p.Usuario)
            .Include(cp => cp.Chat)
                .ThenInclude(c => c.Mensajes.OrderByDescending(m => m.FechaEnvio).Take(1))
            .Where(cp => cp.UsuarioId == userId && !cp.Archivado)
            .OrderByDescending(cp => cp.Chat.UltimaActividad)
            .Select(cp => new ChatDTO
            {
                Id = cp.Chat.Id,
                Tipo = cp.Chat.Tipo.ToString(),
                FechaCreacion = cp.Chat.FechaCreacion,
                UltimaActividad = cp.Chat.UltimaActividad,
                NombreGrupo = cp.Chat.NombreGrupo,
                ImagenGrupo = cp.Chat.ImagenGrupo,
                DescripcionGrupo = cp.Chat.DescripcionGrupo,
                Silenciado = cp.Silenciado && (cp.SilenciadoHasta == null || cp.SilenciadoHasta > DateTime.UtcNow),
                SilenciadoHasta = cp.SilenciadoHasta,
                Archivado = cp.Archivado,
                OtroParticipante = cp.Chat.Tipo == TipoChat.Individual
                    ? cp.Chat.Participantes
                        .Where(p => p.UsuarioId != userId)
                        .Select(p => new UsuarioDTO
                        {
                            Id = p.Usuario.Id,
                            NumeroTelefono = p.Usuario.NumeroTelefono,
                            Nombre = p.Usuario.Nombre,
                            FotoPerfil = p.Usuario.FotoPerfil,
                            Estado = p.Usuario.Estado,
                            EstaEnLinea = p.Usuario.EstaEnLinea,
                            UltimaConexion = p.Usuario.UltimaConexion
                        })
                        .FirstOrDefault()
                    : null,
                UltimoMensaje = cp.Chat.Mensajes
                    .OrderByDescending(m => m.FechaEnvio)
                    .Select(m => new MensajeDTO
                    {
                        Id = m.Id,
                        ChatId = m.ChatId,
                        RemitenteId = m.RemitenteId,
                        Contenido = m.EliminadoParaTodos ? "Mensaje eliminado" : m.Contenido,
                        Tipo = m.Tipo.ToString(),
                        FechaEnvio = m.FechaEnvio,
                        Eliminado = m.Eliminado,
                        EliminadoParaTodos = m.EliminadoParaTodos
                    })
                    .FirstOrDefault(),
                Participantes = cp.Chat.Participantes
                    .Select(p => new ParticipanteDTO
                    {
                        UsuarioId = p.UsuarioId,
                        Nombre = p.Usuario.Nombre,
                        FotoPerfil = p.Usuario.FotoPerfil,
                        Rol = p.Rol.ToString(),
                        EstaEnLinea = p.Usuario.EstaEnLinea
                    })
                    .ToList()
            })
            .ToListAsync();

        return Ok(chats);
    }

    // GET: api/chats/archivados
    [HttpGet("archivados")]
    public async Task<ActionResult<List<ChatDTO>>> GetArchivedChats()
    {
        var userId = GetCurrentUserId();

        var chats = await _context.ChatParticipantes
            .Include(cp => cp.Chat)
                .ThenInclude(c => c.Participantes)
                    .ThenInclude(p => p.Usuario)
            .Include(cp => cp.Chat)
                .ThenInclude(c => c.Mensajes.OrderByDescending(m => m.FechaEnvio).Take(1))
            .Where(cp => cp.UsuarioId == userId && cp.Archivado)
            .OrderByDescending(cp => cp.Chat.UltimaActividad)
            .Select(cp => new ChatDTO
            {
                Id = cp.Chat.Id,
                Tipo = cp.Chat.Tipo.ToString(),
                FechaCreacion = cp.Chat.FechaCreacion,
                UltimaActividad = cp.Chat.UltimaActividad,
                NombreGrupo = cp.Chat.NombreGrupo,
                ImagenGrupo = cp.Chat.ImagenGrupo,
                DescripcionGrupo = cp.Chat.DescripcionGrupo,
                Silenciado = cp.Silenciado && (cp.SilenciadoHasta == null || cp.SilenciadoHasta > DateTime.UtcNow),
                SilenciadoHasta = cp.SilenciadoHasta,
                Archivado = cp.Archivado,
                OtroParticipante = cp.Chat.Tipo == TipoChat.Individual
                    ? cp.Chat.Participantes
                        .Where(p => p.UsuarioId != userId)
                        .Select(p => new UsuarioDTO
                        {
                            Id = p.Usuario.Id,
                            NumeroTelefono = p.Usuario.NumeroTelefono,
                            Nombre = p.Usuario.Nombre,
                            FotoPerfil = p.Usuario.FotoPerfil,
                            Estado = p.Usuario.Estado,
                            EstaEnLinea = p.Usuario.EstaEnLinea,
                            UltimaConexion = p.Usuario.UltimaConexion
                        })
                        .FirstOrDefault()
                    : null,
                UltimoMensaje = cp.Chat.Mensajes
                    .OrderByDescending(m => m.FechaEnvio)
                    .Select(m => new MensajeDTO
                    {
                        Id = m.Id,
                        ChatId = m.ChatId,
                        RemitenteId = m.RemitenteId,
                        Contenido = m.EliminadoParaTodos ? "Mensaje eliminado" : m.Contenido,
                        Tipo = m.Tipo.ToString(),
                        FechaEnvio = m.FechaEnvio,
                        Eliminado = m.Eliminado,
                        EliminadoParaTodos = m.EliminadoParaTodos
                    })
                    .FirstOrDefault(),
                Participantes = cp.Chat.Participantes
                    .Select(p => new ParticipanteDTO
                    {
                        UsuarioId = p.UsuarioId,
                        Nombre = p.Usuario.Nombre,
                        FotoPerfil = p.Usuario.FotoPerfil,
                        Rol = p.Rol.ToString(),
                        EstaEnLinea = p.Usuario.EstaEnLinea
                    })
                    .ToList()
            })
            .ToListAsync();

        return Ok(chats);
    }

    // PUT: api/chats/{id}/silenciar
    [HttpPut("{id}/silenciar")]
    public async Task<ActionResult> MuteChat(Guid id, [FromBody] SilenciarChatDTO dto)
    {
        var userId = GetCurrentUserId();

        var participante = await _context.ChatParticipantes
            .FirstOrDefaultAsync(cp => cp.ChatId == id && cp.UsuarioId == userId);

        if (participante == null)
            return NotFound("Chat no encontrado");

        if (dto.Silenciar)
        {
            participante.Silenciado = true;
            participante.SilenciadoHasta = dto.Duracion switch
            {
                "8h" => DateTime.UtcNow.AddHours(8),
                "1w" => DateTime.UtcNow.AddDays(7),
                "always" => null,
                _ => null
            };
        }
        else
        {
            participante.Silenciado = false;
            participante.SilenciadoHasta = null;
        }

        await _context.SaveChangesAsync();

        return Ok(new
        {
            Silenciado = participante.Silenciado,
            SilenciadoHasta = participante.SilenciadoHasta
        });
    }

    // PUT: api/chats/{id}/archivar
    [HttpPut("{id}/archivar")]
    public async Task<ActionResult> ArchiveChat(Guid id, [FromBody] ArchivarChatDTO dto)
    {
        var userId = GetCurrentUserId();

        var participante = await _context.ChatParticipantes
            .FirstOrDefaultAsync(cp => cp.ChatId == id && cp.UsuarioId == userId);

        if (participante == null)
            return NotFound("Chat no encontrado");

        participante.Archivado = dto.Archivar;
        await _context.SaveChangesAsync();

        return Ok(new { Archivado = participante.Archivado });
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<ChatDTO>> GetChat(Guid id)
    {
        var userId = GetCurrentUserId();

        var chat = await _context.Chats
            .Include(c => c.Participantes)
                .ThenInclude(p => p.Usuario)
            .FirstOrDefaultAsync(c => c.Id == id && c.Participantes.Any(p => p.UsuarioId == userId));

        if (chat == null)
        {
            return NotFound();
        }

        return Ok(new ChatDTO
        {
            Id = chat.Id,
            Tipo = chat.Tipo.ToString(),
            FechaCreacion = chat.FechaCreacion,
            UltimaActividad = chat.UltimaActividad,
            NombreGrupo = chat.NombreGrupo,
            ImagenGrupo = chat.ImagenGrupo,
            DescripcionGrupo = chat.DescripcionGrupo,
            OtroParticipante = chat.Tipo == TipoChat.Individual
                ? chat.Participantes
                    .Where(p => p.UsuarioId != userId)
                    .Select(p => new UsuarioDTO
                    {
                        Id = p.Usuario.Id,
                        NumeroTelefono = p.Usuario.NumeroTelefono,
                        Nombre = p.Usuario.Nombre,
                        FotoPerfil = p.Usuario.FotoPerfil,
                        Estado = p.Usuario.Estado,
                        EstaEnLinea = p.Usuario.EstaEnLinea,
                        UltimaConexion = p.Usuario.UltimaConexion
                    })
                    .FirstOrDefault()
                : null,
            Participantes = chat.Participantes
                .Select(p => new ParticipanteDTO
                {
                    UsuarioId = p.UsuarioId,
                    Nombre = p.Usuario.Nombre,
                    FotoPerfil = p.Usuario.FotoPerfil,
                    Rol = p.Rol.ToString(),
                    EstaEnLinea = p.Usuario.EstaEnLinea
                })
                .ToList()
        });
    }

    [HttpPost("individual")]
    public async Task<ActionResult<ChatDTO>> CreateIndividualChat([FromBody] CrearChatIndividualDTO dto)
    {
        var userId = GetCurrentUserId();

        // Verificar si ya existe un chat individual entre estos usuarios
        var existingChat = await _context.Chats
            .Include(c => c.Participantes)
                .ThenInclude(p => p.Usuario)
            .Where(c => c.Tipo == TipoChat.Individual)
            .Where(c => c.Participantes.Any(p => p.UsuarioId == userId))
            .Where(c => c.Participantes.Any(p => p.UsuarioId == dto.ContactoId))
            .FirstOrDefaultAsync();

        if (existingChat != null)
        {
            return Ok(new ChatDTO
            {
                Id = existingChat.Id,
                Tipo = existingChat.Tipo.ToString(),
                FechaCreacion = existingChat.FechaCreacion,
                UltimaActividad = existingChat.UltimaActividad,
                OtroParticipante = existingChat.Participantes
                    .Where(p => p.UsuarioId != userId)
                    .Select(p => new UsuarioDTO
                    {
                        Id = p.Usuario.Id,
                        NumeroTelefono = p.Usuario.NumeroTelefono,
                        Nombre = p.Usuario.Nombre,
                        FotoPerfil = p.Usuario.FotoPerfil,
                        Estado = p.Usuario.Estado,
                        EstaEnLinea = p.Usuario.EstaEnLinea,
                        UltimaConexion = p.Usuario.UltimaConexion
                    })
                    .FirstOrDefault()
            });
        }

        // Crear nuevo chat
        var chat = new Chat
        {
            Id = Guid.NewGuid(),
            Tipo = TipoChat.Individual,
            FechaCreacion = DateTime.UtcNow
        };

        _context.Chats.Add(chat);

        // Agregar participantes
        _context.ChatParticipantes.Add(new ChatParticipante
        {
            Id = Guid.NewGuid(),
            ChatId = chat.Id,
            UsuarioId = userId,
            FechaUnion = DateTime.UtcNow
        });

        _context.ChatParticipantes.Add(new ChatParticipante
        {
            Id = Guid.NewGuid(),
            ChatId = chat.Id,
            UsuarioId = dto.ContactoId,
            FechaUnion = DateTime.UtcNow
        });

        await _context.SaveChangesAsync();

        // Cargar datos del otro participante
        var otroUsuario = await _context.Usuarios.FindAsync(dto.ContactoId);

        return Ok(new ChatDTO
        {
            Id = chat.Id,
            Tipo = chat.Tipo.ToString(),
            FechaCreacion = chat.FechaCreacion,
            OtroParticipante = otroUsuario != null ? new UsuarioDTO
            {
                Id = otroUsuario.Id,
                NumeroTelefono = otroUsuario.NumeroTelefono,
                Nombre = otroUsuario.Nombre,
                FotoPerfil = otroUsuario.FotoPerfil,
                Estado = otroUsuario.Estado,
                EstaEnLinea = otroUsuario.EstaEnLinea,
                UltimaConexion = otroUsuario.UltimaConexion
            } : null
        });
    }

    [HttpPost("grupo")]
    public async Task<ActionResult<ChatDTO>> CreateGroup([FromBody] CrearGrupoDTO dto)
    {
        var userId = GetCurrentUserId();

        var chat = new Chat
        {
            Id = Guid.NewGuid(),
            Tipo = TipoChat.Grupo,
            NombreGrupo = dto.Nombre,
            DescripcionGrupo = dto.Descripcion,
            FechaCreacion = DateTime.UtcNow
        };

        _context.Chats.Add(chat);

        // Agregar creador como admin
        _context.ChatParticipantes.Add(new ChatParticipante
        {
            Id = Guid.NewGuid(),
            ChatId = chat.Id,
            UsuarioId = userId,
            Rol = RolParticipante.Admin,
            FechaUnion = DateTime.UtcNow
        });

        // Agregar otros participantes
        foreach (var participanteId in dto.ParticipantesIds)
        {
            _context.ChatParticipantes.Add(new ChatParticipante
            {
                Id = Guid.NewGuid(),
                ChatId = chat.Id,
                UsuarioId = participanteId,
                Rol = RolParticipante.Participante,
                FechaUnion = DateTime.UtcNow
            });
        }

        // Obtener nombre del creador para mensaje de sistema
        var creador = await _context.Usuarios.FindAsync(userId);
        var creadorNombre = creador?.Nombre ?? "Usuario";

        // Mensaje de sistema: grupo creado
        await CreateSystemMessage(chat.Id, userId, $"{creadorNombre} creó el grupo \"{dto.Nombre}\"");

        await _context.SaveChangesAsync();

        return Ok(new ChatDTO
        {
            Id = chat.Id,
            Tipo = chat.Tipo.ToString(),
            FechaCreacion = chat.FechaCreacion,
            NombreGrupo = chat.NombreGrupo,
            DescripcionGrupo = chat.DescripcionGrupo
        });
    }

    [HttpPut("grupo/{id}")]
    public async Task<ActionResult<ChatDTO>> UpdateGroup(Guid id, [FromBody] ActualizarGrupoDTO dto)
    {
        var userId = GetCurrentUserId();

        var chat = await _context.Chats
            .Include(c => c.Participantes)
            .FirstOrDefaultAsync(c => c.Id == id && c.Tipo == TipoChat.Grupo);

        if (chat == null)
            return NotFound("Grupo no encontrado");

        // Verificar que el usuario es admin
        var participante = chat.Participantes.FirstOrDefault(p => p.UsuarioId == userId);
        if (participante == null)
            return Forbid();

        if (participante.Rol != RolParticipante.Admin)
            return BadRequest("Solo los administradores pueden modificar el grupo");

        if (!string.IsNullOrWhiteSpace(dto.Nombre))
            chat.NombreGrupo = dto.Nombre;

        if (dto.Descripcion != null)
            chat.DescripcionGrupo = dto.Descripcion;

        await _context.SaveChangesAsync();

        return Ok(new ChatDTO
        {
            Id = chat.Id,
            Tipo = chat.Tipo.ToString(),
            FechaCreacion = chat.FechaCreacion,
            NombreGrupo = chat.NombreGrupo,
            DescripcionGrupo = chat.DescripcionGrupo
        });
    }

    [HttpPost("grupo/{id}/imagen")]
    public async Task<ActionResult<ChatDTO>> UpdateGroupImage(Guid id, IFormFile imagen)
    {
        var userId = GetCurrentUserId();

        var chat = await _context.Chats
            .Include(c => c.Participantes)
            .FirstOrDefaultAsync(c => c.Id == id && c.Tipo == TipoChat.Grupo);

        if (chat == null)
            return NotFound("Grupo no encontrado");

        var participante = chat.Participantes.FirstOrDefault(p => p.UsuarioId == userId);
        if (participante == null)
            return Forbid();

        if (participante.Rol != RolParticipante.Admin)
            return BadRequest("Solo los administradores pueden modificar la imagen del grupo");

        if (imagen.Length > 5 * 1024 * 1024)
            return BadRequest("La imagen no puede superar 5MB");

        var uploadsFolder = Path.Combine(Directory.GetCurrentDirectory(), "uploads", "grupos");
        Directory.CreateDirectory(uploadsFolder);

        var fileName = $"{id}_{DateTime.UtcNow.Ticks}{Path.GetExtension(imagen.FileName)}";
        var filePath = Path.Combine(uploadsFolder, fileName);

        using (var stream = new FileStream(filePath, FileMode.Create))
        {
            await imagen.CopyToAsync(stream);
        }

        chat.ImagenGrupo = $"/uploads/grupos/{fileName}";
        await _context.SaveChangesAsync();

        return Ok(new ChatDTO
        {
            Id = chat.Id,
            Tipo = chat.Tipo.ToString(),
            NombreGrupo = chat.NombreGrupo,
            ImagenGrupo = chat.ImagenGrupo,
            DescripcionGrupo = chat.DescripcionGrupo
        });
    }

    [HttpPost("grupo/{id}/participantes")]
    public async Task<ActionResult> AddParticipants(Guid id, [FromBody] AgregarParticipantesDTO dto)
    {
        var userId = GetCurrentUserId();

        var chat = await _context.Chats
            .Include(c => c.Participantes)
            .FirstOrDefaultAsync(c => c.Id == id && c.Tipo == TipoChat.Grupo);

        if (chat == null)
            return NotFound("Grupo no encontrado");

        var participante = chat.Participantes.FirstOrDefault(p => p.UsuarioId == userId);
        if (participante == null)
            return Forbid();

        if (participante.Rol != RolParticipante.Admin)
            return BadRequest("Solo los administradores pueden agregar participantes");

        var existingIds = chat.Participantes.Select(p => p.UsuarioId).ToHashSet();
        var newParticipants = dto.ParticipantesIds.Where(pid => !existingIds.Contains(pid)).ToList();

        var admin = await _context.Usuarios.FindAsync(userId);
        var adminNombre = admin?.Nombre ?? "Un administrador";

        var agregados = new List<ParticipanteDTO>();

        foreach (var pid in newParticipants)
        {
            var usuario = await _context.Usuarios.FindAsync(pid);
            if (usuario != null)
            {
                _context.ChatParticipantes.Add(new ChatParticipante
                {
                    Id = Guid.NewGuid(),
                    ChatId = id,
                    UsuarioId = pid,
                    Rol = RolParticipante.Participante,
                    FechaUnion = DateTime.UtcNow
                });

                var participanteDto = new ParticipanteDTO
                {
                    UsuarioId = usuario.Id,
                    Nombre = usuario.Nombre,
                    FotoPerfil = usuario.FotoPerfil,
                    Rol = "Participante",
                    EstaEnLinea = usuario.EstaEnLinea
                };
                agregados.Add(participanteDto);

                // Mensaje de sistema
                await CreateSystemMessage(id, userId, $"{adminNombre} agregó a {usuario.Nombre}");

                // Notificar por SignalR
                await _hubContext.Clients.Group(id.ToString()).SendAsync("ParticipantAdded", new
                {
                    ChatId = id,
                    Participante = participanteDto
                });

                // Agregar al usuario al grupo de SignalR si está conectado
                var connectionId = ChatHub.GetConnectionId(pid.ToString());
                if (connectionId != null)
                {
                    await _hubContext.Groups.AddToGroupAsync(connectionId, id.ToString());
                }
            }
        }

        chat.UltimaActividad = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return Ok(new { Message = $"Se agregaron {agregados.Count} participantes", ParticipantesAgregados = agregados });
    }

    [HttpDelete("grupo/{id}/participantes/{usuarioId}")]
    public async Task<ActionResult> RemoveParticipant(Guid id, Guid usuarioId)
    {
        var userId = GetCurrentUserId();

        var chat = await _context.Chats
            .Include(c => c.Participantes)
            .FirstOrDefaultAsync(c => c.Id == id && c.Tipo == TipoChat.Grupo);

        if (chat == null)
            return NotFound("Grupo no encontrado");

        var adminParticipante = chat.Participantes.FirstOrDefault(p => p.UsuarioId == userId);
        if (adminParticipante == null)
            return Forbid();

        if (adminParticipante.Rol != RolParticipante.Admin)
            return BadRequest("Solo los administradores pueden eliminar participantes");

        var participanteAEliminar = chat.Participantes.FirstOrDefault(p => p.UsuarioId == usuarioId);
        if (participanteAEliminar == null)
            return NotFound("El usuario no es participante del grupo");

        // No se puede eliminar al creador/único admin
        var admins = chat.Participantes.Where(p => p.Rol == RolParticipante.Admin).ToList();
        if (participanteAEliminar.Rol == RolParticipante.Admin && admins.Count == 1)
            return BadRequest("No se puede eliminar al único administrador del grupo");

        // Obtener nombres para el mensaje de sistema
        var admin = await _context.Usuarios.FindAsync(userId);
        var usuarioEliminado = await _context.Usuarios.FindAsync(usuarioId);
        var adminNombre = admin?.Nombre ?? "Un administrador";
        var usuarioNombre = usuarioEliminado?.Nombre ?? "Un usuario";

        _context.ChatParticipantes.Remove(participanteAEliminar);

        // Mensaje de sistema
        await CreateSystemMessage(id, userId, $"{adminNombre} eliminó a {usuarioNombre}");

        chat.UltimaActividad = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        // Notificar por SignalR
        await _hubContext.Clients.Group(id.ToString()).SendAsync("ParticipantRemoved", new
        {
            ChatId = id,
            UsuarioId = usuarioId,
            Nombre = usuarioNombre
        });

        // Remover del grupo de SignalR si está conectado
        var connectionId = ChatHub.GetConnectionId(usuarioId.ToString());
        if (connectionId != null)
        {
            await _hubContext.Groups.RemoveFromGroupAsync(connectionId, id.ToString());
        }

        return Ok(new { Message = "Participante eliminado del grupo" });
    }

    [HttpPut("grupo/{id}/participantes/{usuarioId}/rol")]
    public async Task<ActionResult> ChangeParticipantRole(Guid id, Guid usuarioId, [FromBody] CambiarRolDTO dto)
    {
        var userId = GetCurrentUserId();

        var chat = await _context.Chats
            .Include(c => c.Participantes)
            .FirstOrDefaultAsync(c => c.Id == id && c.Tipo == TipoChat.Grupo);

        if (chat == null)
            return NotFound("Grupo no encontrado");

        var adminParticipante = chat.Participantes.FirstOrDefault(p => p.UsuarioId == userId);
        if (adminParticipante == null)
            return Forbid();

        if (adminParticipante.Rol != RolParticipante.Admin)
            return BadRequest("Solo los administradores pueden cambiar roles");

        var participante = chat.Participantes.FirstOrDefault(p => p.UsuarioId == usuarioId);
        if (participante == null)
            return NotFound("El usuario no es participante del grupo");

        if (!Enum.TryParse<RolParticipante>(dto.Rol, true, out var nuevoRol))
            return BadRequest("Rol no válido. Use 'Admin' o 'Participante'");

        // Verificar que no se quede sin admins
        if (participante.Rol == RolParticipante.Admin && nuevoRol == RolParticipante.Participante)
        {
            var admins = chat.Participantes.Where(p => p.Rol == RolParticipante.Admin).ToList();
            if (admins.Count == 1)
                return BadRequest("Debe haber al menos un administrador en el grupo");
        }

        participante.Rol = nuevoRol;
        await _context.SaveChangesAsync();

        return Ok(new { Message = $"Rol cambiado a {nuevoRol}", UsuarioId = usuarioId, NuevoRol = nuevoRol.ToString() });
    }

    [HttpPost("grupo/{id}/salir")]
    public async Task<ActionResult> LeaveGroup(Guid id)
    {
        var userId = GetCurrentUserId();

        var chat = await _context.Chats
            .Include(c => c.Participantes)
            .FirstOrDefaultAsync(c => c.Id == id && c.Tipo == TipoChat.Grupo);

        if (chat == null)
            return NotFound("Grupo no encontrado");

        var participante = chat.Participantes.FirstOrDefault(p => p.UsuarioId == userId);
        if (participante == null)
            return BadRequest("No eres participante de este grupo");

        // Obtener nombre del usuario
        var usuario = await _context.Usuarios.FindAsync(userId);
        var usuarioNombre = usuario?.Nombre ?? "Un usuario";

        // Si es el único admin, debe asignar otro antes de salir
        if (participante.Rol == RolParticipante.Admin)
        {
            var admins = chat.Participantes.Where(p => p.Rol == RolParticipante.Admin).ToList();
            if (admins.Count == 1 && chat.Participantes.Count > 1)
            {
                // Asignar admin al siguiente participante más antiguo
                var nuevoAdmin = chat.Participantes
                    .Where(p => p.UsuarioId != userId)
                    .OrderBy(p => p.FechaUnion)
                    .FirstOrDefault();

                if (nuevoAdmin != null)
                    nuevoAdmin.Rol = RolParticipante.Admin;
            }
        }

        _context.ChatParticipantes.Remove(participante);

        // Si no quedan participantes, eliminar el grupo
        if (chat.Participantes.Count == 1)
        {
            _context.Chats.Remove(chat);
        }
        else
        {
            // Mensaje de sistema
            await CreateSystemMessage(id, userId, $"{usuarioNombre} salió del grupo");
            chat.UltimaActividad = DateTime.UtcNow;
        }

        await _context.SaveChangesAsync();

        // Notificar por SignalR
        await _hubContext.Clients.Group(id.ToString()).SendAsync("ParticipantLeft", new
        {
            ChatId = id,
            UsuarioId = userId,
            Nombre = usuarioNombre
        });

        // Remover del grupo de SignalR
        var connectionId = ChatHub.GetConnectionId(userId.ToString());
        if (connectionId != null)
        {
            await _hubContext.Groups.RemoveFromGroupAsync(connectionId, id.ToString());
        }

        return Ok(new { Message = "Has salido del grupo" });
    }

    [HttpGet("grupo/{id}/participantes")]
    public async Task<ActionResult<List<ParticipanteDTO>>> GetGroupParticipants(Guid id)
    {
        var userId = GetCurrentUserId();

        var chat = await _context.Chats
            .Include(c => c.Participantes)
                .ThenInclude(p => p.Usuario)
            .FirstOrDefaultAsync(c => c.Id == id && c.Tipo == TipoChat.Grupo);

        if (chat == null)
            return NotFound("Grupo no encontrado");

        var esParticipante = chat.Participantes.Any(p => p.UsuarioId == userId);
        if (!esParticipante)
            return Forbid();

        var participantes = chat.Participantes
            .OrderByDescending(p => p.Rol)
            .ThenBy(p => p.FechaUnion)
            .Select(p => new ParticipanteDTO
            {
                UsuarioId = p.UsuarioId,
                Nombre = p.Usuario.Nombre,
                FotoPerfil = p.Usuario.FotoPerfil,
                Rol = p.Rol.ToString(),
                EstaEnLinea = p.Usuario.EstaEnLinea
            })
            .ToList();

        return Ok(participantes);
    }
}
