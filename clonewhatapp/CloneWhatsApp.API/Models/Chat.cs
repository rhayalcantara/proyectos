using System.ComponentModel.DataAnnotations;

namespace CloneWhatsApp.API.Models;

public enum TipoChat
{
    Individual,
    Grupo
}

public class Chat
{
    [Key]
    public Guid Id { get; set; }

    public TipoChat Tipo { get; set; } = TipoChat.Individual;

    public DateTime FechaCreacion { get; set; } = DateTime.UtcNow;

    public DateTime? UltimaActividad { get; set; }

    // Para grupos
    [MaxLength(100)]
    public string? NombreGrupo { get; set; }

    [MaxLength(500)]
    public string? ImagenGrupo { get; set; }

    [MaxLength(500)]
    public string? DescripcionGrupo { get; set; }

    // Navigation properties
    public ICollection<ChatParticipante> Participantes { get; set; } = new List<ChatParticipante>();
    public ICollection<Mensaje> Mensajes { get; set; } = new List<Mensaje>();
}
