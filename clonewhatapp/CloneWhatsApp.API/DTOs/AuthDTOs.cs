using System.ComponentModel.DataAnnotations;

namespace CloneWhatsApp.API.DTOs;

public class RegisterDTO
{
    [Required]
    [Phone]
    [MaxLength(20)]
    public string NumeroTelefono { get; set; } = string.Empty;

    [Required]
    [MaxLength(100)]
    public string Nombre { get; set; } = string.Empty;

    [Required]
    [MinLength(6)]
    public string Password { get; set; } = string.Empty;
}

public class LoginDTO
{
    [Required]
    [Phone]
    public string NumeroTelefono { get; set; } = string.Empty;

    [Required]
    public string Password { get; set; } = string.Empty;
}

public class AuthResponseDTO
{
    public Guid Id { get; set; }
    public string NumeroTelefono { get; set; } = string.Empty;
    public string Nombre { get; set; } = string.Empty;
    public string? FotoPerfil { get; set; }
    public string Token { get; set; } = string.Empty;
    public DateTime TokenExpiration { get; set; }
}

public class UsuarioDTO
{
    public Guid Id { get; set; }
    public string NumeroTelefono { get; set; } = string.Empty;
    public string Nombre { get; set; } = string.Empty;
    public string? FotoPerfil { get; set; }
    public string Estado { get; set; } = string.Empty;
    public DateTime UltimaConexion { get; set; }
    public bool EstaEnLinea { get; set; }
}

public class UpdatePerfilDTO
{
    [MaxLength(100)]
    public string? Nombre { get; set; }

    [MaxLength(150)]
    public string? Estado { get; set; }
}

public class BlockStatusDTO
{
    public bool EstaBloqueado { get; set; }
    public bool MeBloquearon { get; set; }
}
