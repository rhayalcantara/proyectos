using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CloneWhatsApp.API.Models;

public enum TipoLlamada
{
    Voz,
    Video
}

public enum EstadoLlamada
{
    EnCurso,
    Completada,
    Perdida,
    Rechazada
}

public class Llamada
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    public Guid LlamadorId { get; set; }

    [Required]
    public Guid ReceptorId { get; set; }

    public TipoLlamada Tipo { get; set; }

    public EstadoLlamada Estado { get; set; } = EstadoLlamada.EnCurso;

    public DateTime FechaInicio { get; set; } = DateTime.UtcNow;

    public DateTime? FechaFin { get; set; }

    public int? DuracionSegundos { get; set; }

    // Navigation properties
    [ForeignKey("LlamadorId")]
    public Usuario Llamador { get; set; } = null!;

    [ForeignKey("ReceptorId")]
    public Usuario Receptor { get; set; } = null!;
}
