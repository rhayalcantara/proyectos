namespace CloneWhatsApp.API.DTOs;

public class EstadoDTO
{
    public Guid Id { get; set; }
    public Guid UsuarioId { get; set; }
    public string UsuarioNombre { get; set; } = string.Empty;
    public string? UsuarioFoto { get; set; }
    public string Tipo { get; set; } = string.Empty;
    public string? Contenido { get; set; }
    public string? UrlArchivo { get; set; }
    public string? ColorFondo { get; set; }
    public DateTime FechaCreacion { get; set; }
    public DateTime FechaExpiracion { get; set; }
    public int TotalVistas { get; set; }
    public bool VioPorMi { get; set; }
}

public class CrearEstadoTextoDTO
{
    public string Contenido { get; set; } = string.Empty;
    public string? ColorFondo { get; set; }
}

public class VistaEstadoDTO
{
    public Guid Id { get; set; }
    public Guid UsuarioId { get; set; }
    public string UsuarioNombre { get; set; } = string.Empty;
    public string? UsuarioFoto { get; set; }
    public DateTime FechaVista { get; set; }
}

public class EstadosContactoDTO
{
    public Guid UsuarioId { get; set; }
    public string UsuarioNombre { get; set; } = string.Empty;
    public string? UsuarioFoto { get; set; }
    public List<EstadoDTO> Estados { get; set; } = new();
    public bool TodosVistos { get; set; }
    public DateTime UltimaActualizacion { get; set; }
}

public class MisEstadosDTO
{
    public List<EstadoDTO> Estados { get; set; } = new();
    public int TotalVistas { get; set; }
}
