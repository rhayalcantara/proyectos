using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using CloneWhatsApp.API.Data;
using CloneWhatsApp.API.DTOs;
using CloneWhatsApp.API.Models;

namespace CloneWhatsApp.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class UsersController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly IWebHostEnvironment _environment;
    private readonly string[] _allowedExtensions = { ".jpg", ".jpeg", ".png", ".gif", ".webp" };
    private const long MaxFileSize = 5 * 1024 * 1024; // 5MB

    public UsersController(ApplicationDbContext context, IWebHostEnvironment environment)
    {
        _context = context;
        _environment = environment;
    }

    private Guid GetCurrentUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.Parse(userIdClaim!);
    }

    [HttpGet("me")]
    public async Task<ActionResult<UsuarioDTO>> GetCurrentUser()
    {
        var userId = GetCurrentUserId();
        var usuario = await _context.Usuarios.FindAsync(userId);

        if (usuario == null)
        {
            return NotFound();
        }

        return Ok(new UsuarioDTO
        {
            Id = usuario.Id,
            NumeroTelefono = usuario.NumeroTelefono,
            Nombre = usuario.Nombre,
            FotoPerfil = usuario.FotoPerfil,
            Estado = usuario.Estado,
            UltimaConexion = usuario.UltimaConexion,
            EstaEnLinea = usuario.EstaEnLinea
        });
    }

    [HttpPut("me")]
    public async Task<ActionResult<UsuarioDTO>> UpdateProfile([FromBody] UpdatePerfilDTO dto)
    {
        var userId = GetCurrentUserId();
        var usuario = await _context.Usuarios.FindAsync(userId);

        if (usuario == null)
        {
            return NotFound();
        }

        if (!string.IsNullOrEmpty(dto.Nombre))
        {
            usuario.Nombre = dto.Nombre;
        }

        if (dto.Estado != null)
        {
            usuario.Estado = dto.Estado;
        }

        await _context.SaveChangesAsync();

        return Ok(new UsuarioDTO
        {
            Id = usuario.Id,
            NumeroTelefono = usuario.NumeroTelefono,
            Nombre = usuario.Nombre,
            FotoPerfil = usuario.FotoPerfil,
            Estado = usuario.Estado,
            UltimaConexion = usuario.UltimaConexion,
            EstaEnLinea = usuario.EstaEnLinea
        });
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<UsuarioDTO>> GetUser(Guid id)
    {
        var usuario = await _context.Usuarios.FindAsync(id);

        if (usuario == null)
        {
            return NotFound();
        }

        return Ok(new UsuarioDTO
        {
            Id = usuario.Id,
            NumeroTelefono = usuario.NumeroTelefono,
            Nombre = usuario.Nombre,
            FotoPerfil = usuario.FotoPerfil,
            Estado = usuario.Estado,
            UltimaConexion = usuario.UltimaConexion,
            EstaEnLinea = usuario.EstaEnLinea
        });
    }

    [HttpGet("search")]
    public async Task<ActionResult<List<UsuarioDTO>>> SearchUsers([FromQuery] string telefono)
    {
        var usuarios = await _context.Usuarios
            .Where(u => u.NumeroTelefono.Contains(telefono))
            .Take(20)
            .Select(u => new UsuarioDTO
            {
                Id = u.Id,
                NumeroTelefono = u.NumeroTelefono,
                Nombre = u.Nombre,
                FotoPerfil = u.FotoPerfil,
                Estado = u.Estado,
                UltimaConexion = u.UltimaConexion,
                EstaEnLinea = u.EstaEnLinea
            })
            .ToListAsync();

        return Ok(usuarios);
    }

    [HttpPost("me/photo")]
    public async Task<ActionResult<UsuarioDTO>> UploadProfilePhoto(IFormFile file)
    {
        if (file == null || file.Length == 0)
        {
            return BadRequest(new { message = "No se ha seleccionado ningún archivo" });
        }

        if (file.Length > MaxFileSize)
        {
            return BadRequest(new { message = "El archivo es demasiado grande. Máximo 5MB" });
        }

        var extension = Path.GetExtension(file.FileName).ToLowerInvariant();
        if (!_allowedExtensions.Contains(extension))
        {
            return BadRequest(new { message = "Tipo de archivo no permitido. Solo imágenes (jpg, png, gif, webp)" });
        }

        var userId = GetCurrentUserId();
        var usuario = await _context.Usuarios.FindAsync(userId);

        if (usuario == null)
        {
            return NotFound();
        }

        // Eliminar foto anterior si existe
        if (!string.IsNullOrEmpty(usuario.FotoPerfil))
        {
            var oldFilePath = Path.Combine(_environment.WebRootPath, usuario.FotoPerfil.TrimStart('/'));
            if (System.IO.File.Exists(oldFilePath))
            {
                System.IO.File.Delete(oldFilePath);
            }
        }

        // Generar nombre único
        var fileName = $"{userId}_{DateTime.UtcNow.Ticks}{extension}";
        var uploadsFolder = Path.Combine(_environment.WebRootPath, "uploads", "profiles");

        // Asegurar que la carpeta existe
        Directory.CreateDirectory(uploadsFolder);

        var filePath = Path.Combine(uploadsFolder, fileName);

        // Guardar archivo
        using (var stream = new FileStream(filePath, FileMode.Create))
        {
            await file.CopyToAsync(stream);
        }

        // Actualizar usuario
        usuario.FotoPerfil = $"/uploads/profiles/{fileName}";
        await _context.SaveChangesAsync();

        return Ok(new UsuarioDTO
        {
            Id = usuario.Id,
            NumeroTelefono = usuario.NumeroTelefono,
            Nombre = usuario.Nombre,
            FotoPerfil = usuario.FotoPerfil,
            Estado = usuario.Estado,
            UltimaConexion = usuario.UltimaConexion,
            EstaEnLinea = usuario.EstaEnLinea
        });
    }

    [HttpDelete("me/photo")]
    public async Task<ActionResult<UsuarioDTO>> DeleteProfilePhoto()
    {
        var userId = GetCurrentUserId();
        var usuario = await _context.Usuarios.FindAsync(userId);

        if (usuario == null)
        {
            return NotFound();
        }

        // Eliminar foto si existe
        if (!string.IsNullOrEmpty(usuario.FotoPerfil))
        {
            var filePath = Path.Combine(_environment.WebRootPath, usuario.FotoPerfil.TrimStart('/'));
            if (System.IO.File.Exists(filePath))
            {
                System.IO.File.Delete(filePath);
            }
        }

        usuario.FotoPerfil = null;
        await _context.SaveChangesAsync();

        return Ok(new UsuarioDTO
        {
            Id = usuario.Id,
            NumeroTelefono = usuario.NumeroTelefono,
            Nombre = usuario.Nombre,
            FotoPerfil = usuario.FotoPerfil,
            Estado = usuario.Estado,
            UltimaConexion = usuario.UltimaConexion,
            EstaEnLinea = usuario.EstaEnLinea
        });
    }

    // ===== BLOQUEO DE USUARIOS =====

    [HttpGet("blocked")]
    public async Task<ActionResult<List<UsuarioDTO>>> GetBlockedUsers()
    {
        var userId = GetCurrentUserId();

        var usuariosBloqueados = await _context.UsuariosBloqueados
            .Where(ub => ub.UsuarioId == userId)
            .Include(ub => ub.Bloqueado)
            .Select(ub => new UsuarioDTO
            {
                Id = ub.Bloqueado.Id,
                NumeroTelefono = ub.Bloqueado.NumeroTelefono,
                Nombre = ub.Bloqueado.Nombre,
                FotoPerfil = ub.Bloqueado.FotoPerfil,
                Estado = ub.Bloqueado.Estado,
                UltimaConexion = ub.Bloqueado.UltimaConexion,
                EstaEnLinea = ub.Bloqueado.EstaEnLinea
            })
            .ToListAsync();

        return Ok(usuariosBloqueados);
    }

    [HttpPost("block/{userId}")]
    public async Task<ActionResult> BlockUser(Guid userId)
    {
        var currentUserId = GetCurrentUserId();

        if (currentUserId == userId)
        {
            return BadRequest(new { message = "No puedes bloquearte a ti mismo" });
        }

        var usuarioABloquear = await _context.Usuarios.FindAsync(userId);
        if (usuarioABloquear == null)
        {
            return NotFound(new { message = "Usuario no encontrado" });
        }

        // Verificar si ya está bloqueado
        var bloqueoExistente = await _context.UsuariosBloqueados
            .FirstOrDefaultAsync(ub => ub.UsuarioId == currentUserId && ub.BloqueadoId == userId);

        if (bloqueoExistente != null)
        {
            return BadRequest(new { message = "Este usuario ya está bloqueado" });
        }

        var nuevoBloqueo = new UsuarioBloqueado
        {
            Id = Guid.NewGuid(),
            UsuarioId = currentUserId,
            BloqueadoId = userId,
            FechaBloqueo = DateTime.UtcNow
        };

        _context.UsuariosBloqueados.Add(nuevoBloqueo);
        await _context.SaveChangesAsync();

        return Ok(new { message = "Usuario bloqueado exitosamente" });
    }

    [HttpDelete("block/{userId}")]
    public async Task<ActionResult> UnblockUser(Guid userId)
    {
        var currentUserId = GetCurrentUserId();

        var bloqueo = await _context.UsuariosBloqueados
            .FirstOrDefaultAsync(ub => ub.UsuarioId == currentUserId && ub.BloqueadoId == userId);

        if (bloqueo == null)
        {
            return NotFound(new { message = "Este usuario no está bloqueado" });
        }

        _context.UsuariosBloqueados.Remove(bloqueo);
        await _context.SaveChangesAsync();

        return Ok(new { message = "Usuario desbloqueado exitosamente" });
    }

    [HttpGet("{userId}/blocked-status")]
    public async Task<ActionResult<BlockStatusDTO>> GetBlockedStatus(Guid userId)
    {
        var currentUserId = GetCurrentUserId();

        // Verificar si el usuario actual ha bloqueado al otro
        var yoBloqueando = await _context.UsuariosBloqueados
            .AnyAsync(ub => ub.UsuarioId == currentUserId && ub.BloqueadoId == userId);

        // Verificar si el otro usuario ha bloqueado al actual
        var meBloquearon = await _context.UsuariosBloqueados
            .AnyAsync(ub => ub.UsuarioId == userId && ub.BloqueadoId == currentUserId);

        return Ok(new BlockStatusDTO
        {
            EstaBloqueado = yoBloqueando,
            MeBloquearon = meBloquearon
        });
    }
}
