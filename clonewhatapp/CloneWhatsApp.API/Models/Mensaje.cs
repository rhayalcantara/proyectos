using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CloneWhatsApp.API.Models;

public enum TipoMensaje
{
    Texto,
    Imagen,
    Video,
    Audio,
    Documento,
    Ubicacion,
    Contacto,
    Sistema
}

public enum EstadoMensaje
{
    Enviado,
    Entregado,
    Leido
}

public class Mensaje
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    public Guid ChatId { get; set; }

    [Required]
    public Guid RemitenteId { get; set; }

    [MaxLength(4000)]
    public string? Contenido { get; set; }

    public TipoMensaje Tipo { get; set; } = TipoMensaje.Texto;

    [MaxLength(500)]
    public string? UrlArchivo { get; set; }

    [MaxLength(100)]
    public string? NombreArchivo { get; set; }

    public long? TamanoArchivo { get; set; }

    // Para mensajes de audio
    public int? DuracionSegundos { get; set; }

    // Para respuestas
    public Guid? MensajeRespondidoId { get; set; }

    public DateTime FechaEnvio { get; set; } = DateTime.UtcNow;

    public bool Eliminado { get; set; } = false;

    public bool EliminadoParaTodos { get; set; } = false;

    public bool Editado { get; set; } = false;

    // Navigation properties
    [ForeignKey("ChatId")]
    public Chat Chat { get; set; } = null!;

    [ForeignKey("RemitenteId")]
    public Usuario Remitente { get; set; } = null!;

    [ForeignKey("MensajeRespondidoId")]
    public Mensaje? MensajeRespondido { get; set; }

    public ICollection<EstadoMensajeUsuario> Estados { get; set; } = new List<EstadoMensajeUsuario>();
}
