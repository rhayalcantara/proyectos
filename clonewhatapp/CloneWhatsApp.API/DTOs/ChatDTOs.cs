namespace CloneWhatsApp.API.DTOs;

public class ChatDTO
{
    public Guid Id { get; set; }
    public string Tipo { get; set; } = string.Empty;
    public DateTime FechaCreacion { get; set; }
    public DateTime? UltimaActividad { get; set; }

    // Para grupos
    public string? NombreGrupo { get; set; }
    public string? ImagenGrupo { get; set; }
    public string? DescripcionGrupo { get; set; }

    // Para chats individuales
    public UsuarioDTO? OtroParticipante { get; set; }

    // Último mensaje
    public MensajeDTO? UltimoMensaje { get; set; }

    // Mensajes no leídos
    public int MensajesNoLeidos { get; set; }

    // Participantes (para grupos)
    public List<ParticipanteDTO> Participantes { get; set; } = new();

    // Silenciar/Archivar
    public bool Silenciado { get; set; }
    public DateTime? SilenciadoHasta { get; set; }
    public bool Archivado { get; set; }
}

public class ParticipanteDTO
{
    public Guid UsuarioId { get; set; }
    public string Nombre { get; set; } = string.Empty;
    public string? FotoPerfil { get; set; }
    public string Rol { get; set; } = string.Empty;
    public bool EstaEnLinea { get; set; }
}

public class CrearChatIndividualDTO
{
    public Guid ContactoId { get; set; }
}

public class CrearGrupoDTO
{
    public string Nombre { get; set; } = string.Empty;
    public string? Descripcion { get; set; }
    public List<Guid> ParticipantesIds { get; set; } = new();
}

public class ActualizarGrupoDTO
{
    public string? Nombre { get; set; }
    public string? Descripcion { get; set; }
}

public class AgregarParticipantesDTO
{
    public List<Guid> ParticipantesIds { get; set; } = new();
}

public class CambiarRolDTO
{
    public Guid UsuarioId { get; set; }
    public string Rol { get; set; } = string.Empty;
}

public class SilenciarChatDTO
{
    public bool Silenciar { get; set; } = true;
    public string? Duracion { get; set; } // "8h", "1w", "always", null para quitar silencio
}

public class ArchivarChatDTO
{
    public bool Archivar { get; set; } = true;
}
