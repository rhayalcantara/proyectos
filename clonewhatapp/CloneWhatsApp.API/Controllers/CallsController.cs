using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using CloneWhatsApp.API.Data;
using CloneWhatsApp.API.Models;

namespace CloneWhatsApp.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class CallsController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public CallsController(ApplicationDbContext context)
    {
        _context = context;
    }

    private Guid GetCurrentUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.Parse(userIdClaim!);
    }

    // GET: api/calls - Historial de llamadas
    [HttpGet]
    public async Task<ActionResult<List<LlamadaDTO>>> GetCallHistory([FromQuery] int page = 1, [FromQuery] int pageSize = 20)
    {
        var userId = GetCurrentUserId();

        var llamadas = await _context.Llamadas
            .Include(l => l.Llamador)
            .Include(l => l.Receptor)
            .Where(l => l.LlamadorId == userId || l.ReceptorId == userId)
            .OrderByDescending(l => l.FechaInicio)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .Select(l => new LlamadaDTO
            {
                Id = l.Id,
                EsLlamadaSaliente = l.LlamadorId == userId,
                OtroUsuarioId = l.LlamadorId == userId ? l.ReceptorId : l.LlamadorId,
                OtroUsuarioNombre = l.LlamadorId == userId ? l.Receptor.Nombre : l.Llamador.Nombre,
                OtroUsuarioFoto = l.LlamadorId == userId ? l.Receptor.FotoPerfil : l.Llamador.FotoPerfil,
                Tipo = l.Tipo.ToString(),
                Estado = l.Estado.ToString(),
                FechaInicio = l.FechaInicio,
                DuracionSegundos = l.DuracionSegundos
            })
            .ToListAsync();

        return Ok(llamadas);
    }

    // POST: api/calls/start - Iniciar llamada (registrar en historial)
    [HttpPost("start")]
    public async Task<ActionResult<LlamadaDTO>> StartCall([FromBody] IniciarLlamadaDTO dto)
    {
        var userId = GetCurrentUserId();

        // Verificar bloqueos
        var yoBloqueando = await _context.UsuariosBloqueados
            .AnyAsync(ub => ub.UsuarioId == userId && ub.BloqueadoId == dto.ReceptorId);

        if (yoBloqueando)
        {
            return BadRequest(new { message = "No puedes llamar a un usuario que has bloqueado", blocked = true });
        }

        var meBloquearon = await _context.UsuariosBloqueados
            .AnyAsync(ub => ub.UsuarioId == dto.ReceptorId && ub.BloqueadoId == userId);

        if (meBloquearon)
        {
            return BadRequest(new { message = "No puedes llamar a este usuario porque te ha bloqueado", blocked = true });
        }

        var llamada = new Llamada
        {
            Id = Guid.NewGuid(),
            LlamadorId = userId,
            ReceptorId = dto.ReceptorId,
            Tipo = Enum.Parse<TipoLlamada>(dto.Tipo),
            Estado = EstadoLlamada.EnCurso,
            FechaInicio = DateTime.UtcNow
        };

        _context.Llamadas.Add(llamada);
        await _context.SaveChangesAsync();

        var receptor = await _context.Usuarios.FindAsync(dto.ReceptorId);

        return Ok(new LlamadaDTO
        {
            Id = llamada.Id,
            EsLlamadaSaliente = true,
            OtroUsuarioId = dto.ReceptorId,
            OtroUsuarioNombre = receptor?.Nombre,
            OtroUsuarioFoto = receptor?.FotoPerfil,
            Tipo = llamada.Tipo.ToString(),
            Estado = llamada.Estado.ToString(),
            FechaInicio = llamada.FechaInicio
        });
    }

    // PUT: api/calls/{id}/end - Terminar llamada
    [HttpPut("{id}/end")]
    public async Task<ActionResult> EndCall(Guid id, [FromBody] TerminarLlamadaDTO dto)
    {
        var userId = GetCurrentUserId();

        var llamada = await _context.Llamadas.FindAsync(id);
        if (llamada == null)
        {
            return NotFound();
        }

        if (llamada.LlamadorId != userId && llamada.ReceptorId != userId)
        {
            return Forbid();
        }

        llamada.Estado = Enum.Parse<EstadoLlamada>(dto.Estado);
        llamada.FechaFin = DateTime.UtcNow;

        if (llamada.Estado == EstadoLlamada.Completada)
        {
            llamada.DuracionSegundos = (int)(llamada.FechaFin.Value - llamada.FechaInicio).TotalSeconds;
        }

        await _context.SaveChangesAsync();

        return Ok(new { DuracionSegundos = llamada.DuracionSegundos });
    }
}

// DTOs
public class LlamadaDTO
{
    public Guid Id { get; set; }
    public bool EsLlamadaSaliente { get; set; }
    public Guid OtroUsuarioId { get; set; }
    public string? OtroUsuarioNombre { get; set; }
    public string? OtroUsuarioFoto { get; set; }
    public string Tipo { get; set; } = string.Empty;
    public string Estado { get; set; } = string.Empty;
    public DateTime FechaInicio { get; set; }
    public int? DuracionSegundos { get; set; }
}

public class IniciarLlamadaDTO
{
    public Guid ReceptorId { get; set; }
    public string Tipo { get; set; } = "Voz"; // Voz o Video
}

public class TerminarLlamadaDTO
{
    public string Estado { get; set; } = "Completada"; // Completada, Perdida, Rechazada
}
