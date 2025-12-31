using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CloneWhatsApp.API.Models;

public enum RolParticipante
{
    Participante,
    Admin
}

public class ChatParticipante
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    public Guid ChatId { get; set; }

    [Required]
    public Guid UsuarioId { get; set; }

    public RolParticipante Rol { get; set; } = RolParticipante.Participante;

    public bool Silenciado { get; set; } = false;

    public DateTime? SilenciadoHasta { get; set; } // null = silenciado permanentemente

    public bool Archivado { get; set; } = false;

    public DateTime FechaUnion { get; set; } = DateTime.UtcNow;

    public DateTime? UltimaLectura { get; set; }

    // Navigation properties
    [ForeignKey("ChatId")]
    public Chat Chat { get; set; } = null!;

    [ForeignKey("UsuarioId")]
    public Usuario Usuario { get; set; } = null!;
}
