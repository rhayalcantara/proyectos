using System.ComponentModel.DataAnnotations;

namespace CloneWhatsApp.API.Models;

public class SuscripcionPush
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    public Guid UsuarioId { get; set; }

    [Required]
    [MaxLength(500)]
    public string Endpoint { get; set; } = string.Empty;

    [Required]
    [MaxLength(200)]
    public string P256dh { get; set; } = string.Empty;

    [Required]
    [MaxLength(100)]
    public string Auth { get; set; } = string.Empty;

    public DateTime FechaCreacion { get; set; } = DateTime.UtcNow;

    public bool Activo { get; set; } = true;

    // Navigation property
    public Usuario Usuario { get; set; } = null!;
}
