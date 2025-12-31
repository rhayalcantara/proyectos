using CloneWhatsApp.API.DTOs;

namespace CloneWhatsApp.API.Services;

public interface IAuthService
{
    Task<AuthResponseDTO?> RegisterAsync(RegisterDTO dto);
    Task<AuthResponseDTO?> LoginAsync(LoginDTO dto);
    string GenerateJwtToken(Guid userId, string numeroTelefono);
}
