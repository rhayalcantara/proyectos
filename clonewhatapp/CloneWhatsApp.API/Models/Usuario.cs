using System.ComponentModel.DataAnnotations;

namespace CloneWhatsApp.API.Models;

public class Usuario
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    [MaxLength(20)]
    public string NumeroTelefono { get; set; } = string.Empty;

    [Required]
    [MaxLength(100)]
    public string Nombre { get; set; } = string.Empty;

    [Required]
    public string PasswordHash { get; set; } = string.Empty;

    [MaxLength(500)]
    public string? FotoPerfil { get; set; }

    [MaxLength(150)]
    public string Estado { get; set; } = "Hey there! I'm using WhatsApp Clone";

    public DateTime UltimaConexion { get; set; } = DateTime.UtcNow;

    public bool EstaEnLinea { get; set; } = false;

    public DateTime FechaCreacion { get; set; } = DateTime.UtcNow;

    // Navigation properties
    public ICollection<ChatParticipante> ChatsParticipante { get; set; } = new List<ChatParticipante>();
    public ICollection<Mensaje> MensajesEnviados { get; set; } = new List<Mensaje>();
    public ICollection<Contacto> Contactos { get; set; } = new List<Contacto>();
    public ICollection<Contacto> ContactoDe { get; set; } = new List<Contacto>();
    public ICollection<Estado> Estados { get; set; } = new List<Estado>();
    public ICollection<VistaEstado> VistasEstados { get; set; } = new List<VistaEstado>();
    public ICollection<UsuarioBloqueado> UsuariosBloqueados { get; set; } = new List<UsuarioBloqueado>();
    public ICollection<UsuarioBloqueado> BloqueadoPor { get; set; } = new List<UsuarioBloqueado>();
    public ICollection<SuscripcionPush> SuscripcionesPush { get; set; } = new List<SuscripcionPush>();
}
