using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CloneWhatsApp.API.Models;

public class EstadoMensajeUsuario
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    public Guid MensajeId { get; set; }

    [Required]
    public Guid UsuarioId { get; set; }

    public EstadoMensaje Estado { get; set; } = EstadoMensaje.Enviado;

    public DateTime FechaEstado { get; set; } = DateTime.UtcNow;

    // Navigation properties
    [ForeignKey("MensajeId")]
    public Mensaje Mensaje { get; set; } = null!;

    [ForeignKey("UsuarioId")]
    public Usuario Usuario { get; set; } = null!;
}
