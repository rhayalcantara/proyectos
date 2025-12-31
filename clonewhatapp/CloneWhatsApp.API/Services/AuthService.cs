using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using CloneWhatsApp.API.Data;
using CloneWhatsApp.API.DTOs;
using CloneWhatsApp.API.Models;

namespace CloneWhatsApp.API.Services;

public class AuthService : IAuthService
{
    private readonly ApplicationDbContext _context;
    private readonly IConfiguration _configuration;

    public AuthService(ApplicationDbContext context, IConfiguration configuration)
    {
        _context = context;
        _configuration = configuration;
    }

    public async Task<AuthResponseDTO?> RegisterAsync(RegisterDTO dto)
    {
        // Verificar si el número ya existe
        var existingUser = await _context.Usuarios
            .FirstOrDefaultAsync(u => u.NumeroTelefono == dto.NumeroTelefono);

        if (existingUser != null)
        {
            return null;
        }

        var usuario = new Usuario
        {
            Id = Guid.NewGuid(),
            NumeroTelefono = dto.NumeroTelefono,
            Nombre = dto.Nombre,
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(dto.Password),
            FechaCreacion = DateTime.UtcNow
        };

        _context.Usuarios.Add(usuario);
        await _context.SaveChangesAsync();

        var token = GenerateJwtToken(usuario.Id, usuario.NumeroTelefono);
        var expiration = DateTime.UtcNow.AddDays(7);

        return new AuthResponseDTO
        {
            Id = usuario.Id,
            NumeroTelefono = usuario.NumeroTelefono,
            Nombre = usuario.Nombre,
            FotoPerfil = usuario.FotoPerfil,
            Token = token,
            TokenExpiration = expiration
        };
    }

    public async Task<AuthResponseDTO?> LoginAsync(LoginDTO dto)
    {
        var usuario = await _context.Usuarios
            .FirstOrDefaultAsync(u => u.NumeroTelefono == dto.NumeroTelefono);

        if (usuario == null || !BCrypt.Net.BCrypt.Verify(dto.Password, usuario.PasswordHash))
        {
            return null;
        }

        // Actualizar última conexión
        usuario.UltimaConexion = DateTime.UtcNow;
        usuario.EstaEnLinea = true;
        await _context.SaveChangesAsync();

        var token = GenerateJwtToken(usuario.Id, usuario.NumeroTelefono);
        var expiration = DateTime.UtcNow.AddDays(7);

        return new AuthResponseDTO
        {
            Id = usuario.Id,
            NumeroTelefono = usuario.NumeroTelefono,
            Nombre = usuario.Nombre,
            FotoPerfil = usuario.FotoPerfil,
            Token = token,
            TokenExpiration = expiration
        };
    }

    public string GenerateJwtToken(Guid userId, string numeroTelefono)
    {
        var jwtKey = _configuration["Jwt:Key"] ?? throw new InvalidOperationException("JWT Key not configured");
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

        var claims = new[]
        {
            new Claim(ClaimTypes.NameIdentifier, userId.ToString()),
            new Claim(ClaimTypes.MobilePhone, numeroTelefono),
            new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
        };

        var token = new JwtSecurityToken(
            issuer: _configuration["Jwt:Issuer"],
            audience: _configuration["Jwt:Audience"],
            claims: claims,
            expires: DateTime.UtcNow.AddDays(7),
            signingCredentials: credentials
        );

        return new JwtSecurityTokenHandler().WriteToken(token);
    }
}
