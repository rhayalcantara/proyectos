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
public class EstadosController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly IHubContext<ChatHub> _hubContext;
    private readonly IWebHostEnvironment _env;

    public EstadosController(ApplicationDbContext context, IHubContext<ChatHub> hubContext, IWebHostEnvironment env)
    {
        _context = context;
        _hubContext = hubContext;
        _env = env;
    }

    private Guid GetCurrentUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.Parse(userIdClaim!);
    }

    // GET: api/estados - Obtener mis estados
    [HttpGet]
    public async Task<ActionResult<MisEstadosDTO>> GetMyStatuses()
    {
        var userId = GetCurrentUserId();
        var now = DateTime.UtcNow;

        var estados = await _context.Estados
            .Include(e => e.Vistas)
            .Where(e => e.UsuarioId == userId && e.FechaExpiracion > now)
            .OrderByDescending(e => e.FechaCreacion)
            .Select(e => new EstadoDTO
            {
                Id = e.Id,
                UsuarioId = e.UsuarioId,
                Tipo = e.Tipo.ToString(),
                Contenido = e.Contenido,
                UrlArchivo = e.UrlArchivo,
                ColorFondo = e.ColorFondo,
                FechaCreacion = e.FechaCreacion,
                FechaExpiracion = e.FechaExpiracion,
                TotalVistas = e.Vistas.Count
            })
            .ToListAsync();

        return Ok(new MisEstadosDTO
        {
            Estados = estados,
            TotalVistas = estados.Sum(e => e.TotalVistas)
        });
    }

    // GET: api/estados/contactos - Obtener estados de contactos
    [HttpGet("contactos")]
    public async Task<ActionResult<List<EstadosContactoDTO>>> GetContactsStatuses()
    {
        var userId = GetCurrentUserId();
        var now = DateTime.UtcNow;

        // Obtener IDs de usuarios que me bloquearon
        var usuariosQueMeBloquearon = await _context.UsuariosBloqueados
            .Where(ub => ub.BloqueadoId == userId)
            .Select(ub => ub.UsuarioId)
            .ToListAsync();

        // Obtener IDs de usuarios que yo bloqueé
        var usuariosQueBloqueé = await _context.UsuariosBloqueados
            .Where(ub => ub.UsuarioId == userId)
            .Select(ub => ub.BloqueadoId)
            .ToListAsync();

        // Obtener IDs de contactos (excluyendo bloqueados)
        var contactosIds = await _context.Contactos
            .Where(c => c.UsuarioId == userId && !c.Bloqueado)
            .Select(c => c.ContactoUsuarioId)
            .Where(id => !usuariosQueMeBloquearon.Contains(id) && !usuariosQueBloqueé.Contains(id))
            .ToListAsync();

        // Obtener estados de contactos agrupados por usuario
        var estadosPorContacto = await _context.Estados
            .Include(e => e.Usuario)
            .Include(e => e.Vistas)
            .Where(e => contactosIds.Contains(e.UsuarioId) && e.FechaExpiracion > now)
            .OrderByDescending(e => e.FechaCreacion)
            .ToListAsync();

        var estadosAgrupados = estadosPorContacto
            .GroupBy(e => e.UsuarioId)
            .Select(g =>
            {
                var usuario = g.First().Usuario;
                var estados = g.Select(e => new EstadoDTO
                {
                    Id = e.Id,
                    UsuarioId = e.UsuarioId,
                    UsuarioNombre = usuario.Nombre,
                    UsuarioFoto = usuario.FotoPerfil,
                    Tipo = e.Tipo.ToString(),
                    Contenido = e.Contenido,
                    UrlArchivo = e.UrlArchivo,
                    ColorFondo = e.ColorFondo,
                    FechaCreacion = e.FechaCreacion,
                    FechaExpiracion = e.FechaExpiracion,
                    TotalVistas = e.Vistas.Count,
                    VioPorMi = e.Vistas.Any(v => v.UsuarioId == userId)
                }).ToList();

                return new EstadosContactoDTO
                {
                    UsuarioId = usuario.Id,
                    UsuarioNombre = usuario.Nombre,
                    UsuarioFoto = usuario.FotoPerfil,
                    Estados = estados,
                    TodosVistos = estados.All(e => e.VioPorMi),
                    UltimaActualizacion = estados.Max(e => e.FechaCreacion)
                };
            })
            .OrderByDescending(ec => ec.Estados.Any(e => !e.VioPorMi)) // Primero los no vistos
            .ThenByDescending(ec => ec.UltimaActualizacion)
            .ToList();

        return Ok(estadosAgrupados);
    }

    // POST: api/estados - Publicar estado de texto
    [HttpPost]
    public async Task<ActionResult<EstadoDTO>> CreateTextStatus([FromBody] CrearEstadoTextoDTO dto)
    {
        var userId = GetCurrentUserId();

        if (string.IsNullOrWhiteSpace(dto.Contenido))
            return BadRequest("El contenido es requerido");

        var usuario = await _context.Usuarios.FindAsync(userId);
        if (usuario == null)
            return Unauthorized();

        var estado = new Estado
        {
            Id = Guid.NewGuid(),
            UsuarioId = userId,
            Tipo = TipoEstado.Texto,
            Contenido = dto.Contenido,
            ColorFondo = dto.ColorFondo ?? "#128C7E", // Color WhatsApp por defecto
            FechaCreacion = DateTime.UtcNow,
            FechaExpiracion = DateTime.UtcNow.AddHours(24)
        };

        _context.Estados.Add(estado);
        await _context.SaveChangesAsync();

        var estadoDto = new EstadoDTO
        {
            Id = estado.Id,
            UsuarioId = estado.UsuarioId,
            UsuarioNombre = usuario.Nombre,
            UsuarioFoto = usuario.FotoPerfil,
            Tipo = estado.Tipo.ToString(),
            Contenido = estado.Contenido,
            ColorFondo = estado.ColorFondo,
            FechaCreacion = estado.FechaCreacion,
            FechaExpiracion = estado.FechaExpiracion,
            TotalVistas = 0
        };

        // Notificar a contactos por SignalR
        await NotifyContactsNewStatus(userId, estadoDto);

        return Ok(estadoDto);
    }

    // POST: api/estados/imagen - Publicar estado con imagen
    [HttpPost("imagen")]
    public async Task<ActionResult<EstadoDTO>> CreateImageStatus(IFormFile imagen, [FromForm] string? caption)
    {
        var userId = GetCurrentUserId();

        if (imagen == null || imagen.Length == 0)
            return BadRequest("La imagen es requerida");

        if (imagen.Length > 10 * 1024 * 1024) // 10MB
            return BadRequest("La imagen no puede superar 10MB");

        var allowedExtensions = new[] { ".jpg", ".jpeg", ".png", ".gif", ".webp" };
        var extension = Path.GetExtension(imagen.FileName).ToLowerInvariant();
        if (!allowedExtensions.Contains(extension))
            return BadRequest("Formato de imagen no válido");

        var usuario = await _context.Usuarios.FindAsync(userId);
        if (usuario == null)
            return Unauthorized();

        // Guardar imagen
        var uploadsFolder = Path.Combine(Directory.GetCurrentDirectory(), "uploads", "estados");
        Directory.CreateDirectory(uploadsFolder);

        var fileName = $"{userId}_{DateTime.UtcNow.Ticks}{extension}";
        var filePath = Path.Combine(uploadsFolder, fileName);

        using (var stream = new FileStream(filePath, FileMode.Create))
        {
            await imagen.CopyToAsync(stream);
        }

        var estado = new Estado
        {
            Id = Guid.NewGuid(),
            UsuarioId = userId,
            Tipo = TipoEstado.Imagen,
            UrlArchivo = $"/uploads/estados/{fileName}",
            Contenido = caption,
            FechaCreacion = DateTime.UtcNow,
            FechaExpiracion = DateTime.UtcNow.AddHours(24)
        };

        _context.Estados.Add(estado);
        await _context.SaveChangesAsync();

        var estadoDto = new EstadoDTO
        {
            Id = estado.Id,
            UsuarioId = estado.UsuarioId,
            UsuarioNombre = usuario.Nombre,
            UsuarioFoto = usuario.FotoPerfil,
            Tipo = estado.Tipo.ToString(),
            Contenido = estado.Contenido,
            UrlArchivo = estado.UrlArchivo,
            FechaCreacion = estado.FechaCreacion,
            FechaExpiracion = estado.FechaExpiracion,
            TotalVistas = 0
        };

        // Notificar a contactos
        await NotifyContactsNewStatus(userId, estadoDto);

        return Ok(estadoDto);
    }

    // POST: api/estados/{id}/vista - Marcar estado como visto
    [HttpPost("{id}/vista")]
    public async Task<ActionResult> MarkAsViewed(Guid id)
    {
        var userId = GetCurrentUserId();

        var estado = await _context.Estados
            .Include(e => e.Usuario)
            .FirstOrDefaultAsync(e => e.Id == id);

        if (estado == null)
            return NotFound("Estado no encontrado");

        if (estado.FechaExpiracion < DateTime.UtcNow)
            return BadRequest("El estado ha expirado");

        // No registrar vista si es mi propio estado
        if (estado.UsuarioId == userId)
            return Ok(new { Message = "Es tu propio estado" });

        // Verificar si ya lo vio
        var vistaExistente = await _context.VistasEstados
            .AnyAsync(v => v.EstadoId == id && v.UsuarioId == userId);

        if (vistaExistente)
            return Ok(new { Message = "Ya viste este estado" });

        // Registrar vista
        var vista = new VistaEstado
        {
            Id = Guid.NewGuid(),
            EstadoId = id,
            UsuarioId = userId,
            FechaVista = DateTime.UtcNow
        };

        _context.VistasEstados.Add(vista);
        await _context.SaveChangesAsync();

        // Obtener datos del usuario que vio
        var viewer = await _context.Usuarios.FindAsync(userId);

        // Notificar al dueño del estado
        var connectionId = ChatHub.GetConnectionId(estado.UsuarioId.ToString());
        if (connectionId != null)
        {
            await _hubContext.Clients.Client(connectionId).SendAsync("StatusViewed", new
            {
                EstadoId = id,
                Viewer = new VistaEstadoDTO
                {
                    Id = vista.Id,
                    UsuarioId = userId,
                    UsuarioNombre = viewer?.Nombre ?? "Usuario",
                    UsuarioFoto = viewer?.FotoPerfil,
                    FechaVista = vista.FechaVista
                }
            });
        }

        return Ok(new { Message = "Estado marcado como visto" });
    }

    // GET: api/estados/{id}/vistas - Ver quién vio mi estado
    [HttpGet("{id}/vistas")]
    public async Task<ActionResult<List<VistaEstadoDTO>>> GetStatusViewers(Guid id)
    {
        var userId = GetCurrentUserId();

        var estado = await _context.Estados
            .FirstOrDefaultAsync(e => e.Id == id && e.UsuarioId == userId);

        if (estado == null)
            return NotFound("Estado no encontrado o no tienes permiso");

        var vistas = await _context.VistasEstados
            .Include(v => v.Usuario)
            .Where(v => v.EstadoId == id)
            .OrderByDescending(v => v.FechaVista)
            .Select(v => new VistaEstadoDTO
            {
                Id = v.Id,
                UsuarioId = v.UsuarioId,
                UsuarioNombre = v.Usuario.Nombre,
                UsuarioFoto = v.Usuario.FotoPerfil,
                FechaVista = v.FechaVista
            })
            .ToListAsync();

        return Ok(vistas);
    }

    // DELETE: api/estados/{id} - Eliminar mi estado
    [HttpDelete("{id}")]
    public async Task<ActionResult> DeleteStatus(Guid id)
    {
        var userId = GetCurrentUserId();

        var estado = await _context.Estados
            .FirstOrDefaultAsync(e => e.Id == id && e.UsuarioId == userId);

        if (estado == null)
            return NotFound("Estado no encontrado o no tienes permiso");

        // Eliminar archivo si es imagen
        if (estado.Tipo == TipoEstado.Imagen && !string.IsNullOrEmpty(estado.UrlArchivo))
        {
            var filePath = Path.Combine(Directory.GetCurrentDirectory(), estado.UrlArchivo.TrimStart('/'));
            if (System.IO.File.Exists(filePath))
            {
                System.IO.File.Delete(filePath);
            }
        }

        _context.Estados.Remove(estado);
        await _context.SaveChangesAsync();

        // Notificar a contactos
        await NotifyContactsStatusDeleted(userId, id);

        return Ok(new { Message = "Estado eliminado" });
    }

    // Método helper para notificar nuevo estado a contactos
    private async Task NotifyContactsNewStatus(Guid userId, EstadoDTO estado)
    {
        // Obtener IDs de usuarios que me bloquearon o que yo bloqueé
        var usuariosBloqueados = await _context.UsuariosBloqueados
            .Where(ub => ub.UsuarioId == userId || ub.BloqueadoId == userId)
            .Select(ub => ub.UsuarioId == userId ? ub.BloqueadoId : ub.UsuarioId)
            .ToListAsync();

        // Obtener contactos que tienen a este usuario como contacto (excluyendo bloqueados)
        var contactosQueLoTienen = await _context.Contactos
            .Where(c => c.ContactoUsuarioId == userId && !c.Bloqueado)
            .Select(c => c.UsuarioId)
            .Where(id => !usuariosBloqueados.Contains(id))
            .ToListAsync();

        foreach (var contactoId in contactosQueLoTienen)
        {
            var connectionId = ChatHub.GetConnectionId(contactoId.ToString());
            if (connectionId != null)
            {
                await _hubContext.Clients.Client(connectionId).SendAsync("NewStatus", estado);
            }
        }
    }

    // Método helper para notificar eliminación de estado
    private async Task NotifyContactsStatusDeleted(Guid userId, Guid estadoId)
    {
        // Obtener IDs de usuarios que me bloquearon o que yo bloqueé
        var usuariosBloqueados = await _context.UsuariosBloqueados
            .Where(ub => ub.UsuarioId == userId || ub.BloqueadoId == userId)
            .Select(ub => ub.UsuarioId == userId ? ub.BloqueadoId : ub.UsuarioId)
            .ToListAsync();

        var contactosQueLoTienen = await _context.Contactos
            .Where(c => c.ContactoUsuarioId == userId && !c.Bloqueado)
            .Select(c => c.UsuarioId)
            .Where(id => !usuariosBloqueados.Contains(id))
            .ToListAsync();

        foreach (var contactoId in contactosQueLoTienen)
        {
            var connectionId = ChatHub.GetConnectionId(contactoId.ToString());
            if (connectionId != null)
            {
                await _hubContext.Clients.Client(connectionId).SendAsync("StatusDeleted", new
                {
                    UsuarioId = userId,
                    EstadoId = estadoId
                });
            }
        }
    }
}
