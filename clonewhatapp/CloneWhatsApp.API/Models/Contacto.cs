using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CloneWhatsApp.API.Models;

public class Contacto
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    public Guid UsuarioId { get; set; }

    [Required]
    public Guid ContactoUsuarioId { get; set; }

    [MaxLength(100)]
    public string? NombrePersonalizado { get; set; }

    public bool Bloqueado { get; set; } = false;

    public DateTime FechaAgregado { get; set; } = DateTime.UtcNow;

    // Navigation properties
    [ForeignKey("UsuarioId")]
    public Usuario Usuario { get; set; } = null!;

    [ForeignKey("ContactoUsuarioId")]
    public Usuario ContactoUsuario { get; set; } = null!;
}
