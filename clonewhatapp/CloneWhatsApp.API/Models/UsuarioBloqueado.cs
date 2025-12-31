using System.ComponentModel.DataAnnotations;

namespace CloneWhatsApp.API.Models;

public class UsuarioBloqueado
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    public Guid UsuarioId { get; set; }

    [Required]
    public Guid BloqueadoId { get; set; }

    public DateTime FechaBloqueo { get; set; } = DateTime.UtcNow;

    // Navigation properties
    public Usuario Usuario { get; set; } = null!;
    public Usuario Bloqueado { get; set; } = null!;
}
