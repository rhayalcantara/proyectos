using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CloneWhatsApp.API.Models;

public enum TipoEstado
{
    Texto,
    Imagen
}

public class Estado
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    public Guid UsuarioId { get; set; }

    public TipoEstado Tipo { get; set; } = TipoEstado.Texto;

    [MaxLength(1000)]
    public string? Contenido { get; set; }

    [MaxLength(500)]
    public string? UrlArchivo { get; set; }

    [MaxLength(20)]
    public string? ColorFondo { get; set; }

    public DateTime FechaCreacion { get; set; } = DateTime.UtcNow;

    public DateTime FechaExpiracion { get; set; }

    // Navigation properties
    [ForeignKey("UsuarioId")]
    public Usuario Usuario { get; set; } = null!;

    public ICollection<VistaEstado> Vistas { get; set; } = new List<VistaEstado>();
}
