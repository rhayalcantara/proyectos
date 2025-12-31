using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CloneWhatsApp.API.Models;

public class VistaEstado
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    public Guid EstadoId { get; set; }

    [Required]
    public Guid UsuarioId { get; set; }

    public DateTime FechaVista { get; set; } = DateTime.UtcNow;

    // Navigation properties
    [ForeignKey("EstadoId")]
    public Estado Estado { get; set; } = null!;

    [ForeignKey("UsuarioId")]
    public Usuario Usuario { get; set; } = null!;
}
