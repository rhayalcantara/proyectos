using System.ComponentModel.DataAnnotations;

namespace CloneWhatsApp.API.DTOs;

public class ContactoDTO
{
    public Guid Id { get; set; }
    public Guid ContactoUsuarioId { get; set; }
    public string NumeroTelefono { get; set; } = string.Empty;
    public string Nombre { get; set; } = string.Empty;
    public string? NombrePersonalizado { get; set; }
    public string? FotoPerfil { get; set; }
    public string Estado { get; set; } = string.Empty;
    public bool Bloqueado { get; set; }
    public bool EstaEnLinea { get; set; }
    public DateTime UltimaConexion { get; set; }
}

public class AgregarContactoDTO
{
    [Required]
    [Phone]
    public string NumeroTelefono { get; set; } = string.Empty;

    [MaxLength(100)]
    public string? NombrePersonalizado { get; set; }
}

public class ActualizarContactoDTO
{
    [MaxLength(100)]
    public string? NombrePersonalizado { get; set; }

    public bool? Bloqueado { get; set; }
}
