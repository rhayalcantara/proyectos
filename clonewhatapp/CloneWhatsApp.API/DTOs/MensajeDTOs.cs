namespace CloneWhatsApp.API.DTOs;

public class MensajeDTO
{
    public Guid Id { get; set; }
    public Guid ChatId { get; set; }
    public Guid RemitenteId { get; set; }
    public string? RemitenteNombre { get; set; }
    public string? Contenido { get; set; }
    public string Tipo { get; set; } = string.Empty;
    public string? UrlArchivo { get; set; }
    public string? NombreArchivo { get; set; }
    public long? TamanoArchivo { get; set; }
    public int? DuracionSegundos { get; set; }
    public Guid? MensajeRespondidoId { get; set; }
    public MensajeDTO? MensajeRespondido { get; set; }
    public DateTime FechaEnvio { get; set; }
    public bool Eliminado { get; set; }
    public bool EliminadoParaTodos { get; set; }
    public bool Editado { get; set; }
    public string Estado { get; set; } = string.Empty; // Enviado, Entregado, Leido
}

public class EnviarMensajeDTO
{
    public Guid ChatId { get; set; }
    public string? Contenido { get; set; }
    public string Tipo { get; set; } = "Texto";
    public Guid? MensajeRespondidoId { get; set; }
}

public class MensajeEnviadoDTO
{
    public Guid MensajeId { get; set; }
    public Guid ChatId { get; set; }
    public MensajeDTO Mensaje { get; set; } = null!;
}

public class ActualizarEstadoMensajeDTO
{
    public Guid MensajeId { get; set; }
    public string Estado { get; set; } = string.Empty; // Entregado, Leido
}

public class TypingDTO
{
    public Guid ChatId { get; set; }
    public Guid UsuarioId { get; set; }
    public string NombreUsuario { get; set; } = string.Empty;
    public bool EstaEscribiendo { get; set; }
}

public class ForwardMessageDTO
{
    public Guid TargetChatId { get; set; }
}
