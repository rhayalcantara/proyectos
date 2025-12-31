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
public class ContactsController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public ContactsController(ApplicationDbContext context)
    {
        _context = context;
    }

    private Guid GetCurrentUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.Parse(userIdClaim!);
    }

    [HttpGet]
    public async Task<ActionResult<List<ContactoDTO>>> GetContacts()
    {
        var userId = GetCurrentUserId();

        var contactos = await _context.Contactos
            .Include(c => c.ContactoUsuario)
            .Where(c => c.UsuarioId == userId)
            .Select(c => new ContactoDTO
            {
                Id = c.Id,
                ContactoUsuarioId = c.ContactoUsuarioId,
                NumeroTelefono = c.ContactoUsuario.NumeroTelefono,
                Nombre = c.ContactoUsuario.Nombre,
                NombrePersonalizado = c.NombrePersonalizado,
                FotoPerfil = c.ContactoUsuario.FotoPerfil,
                Estado = c.ContactoUsuario.Estado,
                Bloqueado = c.Bloqueado,
                EstaEnLinea = c.ContactoUsuario.EstaEnLinea,
                UltimaConexion = c.ContactoUsuario.UltimaConexion
            })
            .ToListAsync();

        return Ok(contactos);
    }

    [HttpPost]
    public async Task<ActionResult<ContactoDTO>> AddContact([FromBody] AgregarContactoDTO dto)
    {
        var userId = GetCurrentUserId();

        // Buscar usuario por número de teléfono
        var contactoUsuario = await _context.Usuarios
            .FirstOrDefaultAsync(u => u.NumeroTelefono == dto.NumeroTelefono);

        if (contactoUsuario == null)
        {
            return NotFound(new { message = "Usuario no encontrado" });
        }

        if (contactoUsuario.Id == userId)
        {
            return BadRequest(new { message = "No puedes agregarte a ti mismo" });
        }

        // Verificar si ya existe el contacto
        var existingContact = await _context.Contactos
            .FirstOrDefaultAsync(c => c.UsuarioId == userId && c.ContactoUsuarioId == contactoUsuario.Id);

        if (existingContact != null)
        {
            return BadRequest(new { message = "El contacto ya existe" });
        }

        var contacto = new Contacto
        {
            Id = Guid.NewGuid(),
            UsuarioId = userId,
            ContactoUsuarioId = contactoUsuario.Id,
            NombrePersonalizado = dto.NombrePersonalizado,
            FechaAgregado = DateTime.UtcNow
        };

        _context.Contactos.Add(contacto);
        await _context.SaveChangesAsync();

        return Ok(new ContactoDTO
        {
            Id = contacto.Id,
            ContactoUsuarioId = contactoUsuario.Id,
            NumeroTelefono = contactoUsuario.NumeroTelefono,
            Nombre = contactoUsuario.Nombre,
            NombrePersonalizado = contacto.NombrePersonalizado,
            FotoPerfil = contactoUsuario.FotoPerfil,
            Estado = contactoUsuario.Estado,
            Bloqueado = contacto.Bloqueado,
            EstaEnLinea = contactoUsuario.EstaEnLinea,
            UltimaConexion = contactoUsuario.UltimaConexion
        });
    }

    [HttpPut("{id}")]
    public async Task<ActionResult<ContactoDTO>> UpdateContact(Guid id, [FromBody] ActualizarContactoDTO dto)
    {
        var userId = GetCurrentUserId();

        var contacto = await _context.Contactos
            .Include(c => c.ContactoUsuario)
            .FirstOrDefaultAsync(c => c.Id == id && c.UsuarioId == userId);

        if (contacto == null)
        {
            return NotFound();
        }

        if (dto.NombrePersonalizado != null)
        {
            contacto.NombrePersonalizado = dto.NombrePersonalizado;
        }

        if (dto.Bloqueado.HasValue)
        {
            contacto.Bloqueado = dto.Bloqueado.Value;
        }

        await _context.SaveChangesAsync();

        return Ok(new ContactoDTO
        {
            Id = contacto.Id,
            ContactoUsuarioId = contacto.ContactoUsuarioId,
            NumeroTelefono = contacto.ContactoUsuario.NumeroTelefono,
            Nombre = contacto.ContactoUsuario.Nombre,
            NombrePersonalizado = contacto.NombrePersonalizado,
            FotoPerfil = contacto.ContactoUsuario.FotoPerfil,
            Estado = contacto.ContactoUsuario.Estado,
            Bloqueado = contacto.Bloqueado,
            EstaEnLinea = contacto.ContactoUsuario.EstaEnLinea,
            UltimaConexion = contacto.ContactoUsuario.UltimaConexion
        });
    }

    [HttpDelete("{id}")]
    public async Task<ActionResult> DeleteContact(Guid id)
    {
        var userId = GetCurrentUserId();

        var contacto = await _context.Contactos
            .FirstOrDefaultAsync(c => c.Id == id && c.UsuarioId == userId);

        if (contacto == null)
        {
            return NotFound();
        }

        _context.Contactos.Remove(contacto);
        await _context.SaveChangesAsync();

        return NoContent();
    }
}
