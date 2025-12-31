using Microsoft.EntityFrameworkCore;
using CloneWhatsApp.API.Models;

namespace CloneWhatsApp.API.Data;

public class ApplicationDbContext : DbContext
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
        : base(options)
    {
    }

    public DbSet<Usuario> Usuarios { get; set; }
    public DbSet<Contacto> Contactos { get; set; }
    public DbSet<Chat> Chats { get; set; }
    public DbSet<ChatParticipante> ChatParticipantes { get; set; }
    public DbSet<Mensaje> Mensajes { get; set; }
    public DbSet<EstadoMensajeUsuario> EstadosMensaje { get; set; }
    public DbSet<Estado> Estados { get; set; }
    public DbSet<VistaEstado> VistasEstados { get; set; }
    public DbSet<Llamada> Llamadas { get; set; }
    public DbSet<UsuarioBloqueado> UsuariosBloqueados { get; set; }
    public DbSet<SuscripcionPush> SuscripcionesPush { get; set; }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        // Usuario
        modelBuilder.Entity<Usuario>(entity =>
        {
            entity.HasIndex(e => e.NumeroTelefono).IsUnique();
        });

        // Contacto - relaciones
        modelBuilder.Entity<Contacto>(entity =>
        {
            entity.HasOne(c => c.Usuario)
                .WithMany(u => u.Contactos)
                .HasForeignKey(c => c.UsuarioId)
                .OnDelete(DeleteBehavior.Restrict);

            entity.HasOne(c => c.ContactoUsuario)
                .WithMany(u => u.ContactoDe)
                .HasForeignKey(c => c.ContactoUsuarioId)
                .OnDelete(DeleteBehavior.Restrict);

            entity.HasIndex(c => new { c.UsuarioId, c.ContactoUsuarioId }).IsUnique();
        });

        // Chat
        modelBuilder.Entity<Chat>(entity =>
        {
            entity.HasMany(c => c.Participantes)
                .WithOne(p => p.Chat)
                .HasForeignKey(p => p.ChatId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasMany(c => c.Mensajes)
                .WithOne(m => m.Chat)
                .HasForeignKey(m => m.ChatId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        // ChatParticipante
        modelBuilder.Entity<ChatParticipante>(entity =>
        {
            entity.HasIndex(cp => new { cp.ChatId, cp.UsuarioId }).IsUnique();

            entity.HasOne(cp => cp.Usuario)
                .WithMany(u => u.ChatsParticipante)
                .HasForeignKey(cp => cp.UsuarioId)
                .OnDelete(DeleteBehavior.Restrict);
        });

        // Mensaje
        modelBuilder.Entity<Mensaje>(entity =>
        {
            entity.HasOne(m => m.Remitente)
                .WithMany(u => u.MensajesEnviados)
                .HasForeignKey(m => m.RemitenteId)
                .OnDelete(DeleteBehavior.Restrict);

            entity.HasOne(m => m.MensajeRespondido)
                .WithMany()
                .HasForeignKey(m => m.MensajeRespondidoId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.HasIndex(m => m.ChatId);
            entity.HasIndex(m => m.FechaEnvio);
        });

        // EstadoMensajeUsuario
        modelBuilder.Entity<EstadoMensajeUsuario>(entity =>
        {
            entity.HasIndex(e => new { e.MensajeId, e.UsuarioId }).IsUnique();
        });

        // Estado (Stories)
        modelBuilder.Entity<Estado>(entity =>
        {
            entity.HasOne(e => e.Usuario)
                .WithMany(u => u.Estados)
                .HasForeignKey(e => e.UsuarioId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasIndex(e => e.UsuarioId);
            entity.HasIndex(e => e.FechaExpiracion);
        });

        // VistaEstado
        modelBuilder.Entity<VistaEstado>(entity =>
        {
            entity.HasOne(v => v.Estado)
                .WithMany(e => e.Vistas)
                .HasForeignKey(v => v.EstadoId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(v => v.Usuario)
                .WithMany(u => u.VistasEstados)
                .HasForeignKey(v => v.UsuarioId)
                .OnDelete(DeleteBehavior.Restrict);

            entity.HasIndex(v => new { v.EstadoId, v.UsuarioId }).IsUnique();
        });

        // Llamada
        modelBuilder.Entity<Llamada>(entity =>
        {
            entity.HasOne(l => l.Llamador)
                .WithMany()
                .HasForeignKey(l => l.LlamadorId)
                .OnDelete(DeleteBehavior.Restrict);

            entity.HasOne(l => l.Receptor)
                .WithMany()
                .HasForeignKey(l => l.ReceptorId)
                .OnDelete(DeleteBehavior.Restrict);

            entity.HasIndex(l => l.LlamadorId);
            entity.HasIndex(l => l.ReceptorId);
            entity.HasIndex(l => l.FechaInicio);
        });

        // UsuarioBloqueado
        modelBuilder.Entity<UsuarioBloqueado>(entity =>
        {
            entity.HasOne(ub => ub.Usuario)
                .WithMany(u => u.UsuariosBloqueados)
                .HasForeignKey(ub => ub.UsuarioId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(ub => ub.Bloqueado)
                .WithMany(u => u.BloqueadoPor)
                .HasForeignKey(ub => ub.BloqueadoId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasIndex(ub => new { ub.UsuarioId, ub.BloqueadoId }).IsUnique();
        });

        // SuscripcionPush
        modelBuilder.Entity<SuscripcionPush>(entity =>
        {
            entity.HasOne(sp => sp.Usuario)
                .WithMany(u => u.SuscripcionesPush)
                .HasForeignKey(sp => sp.UsuarioId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasIndex(sp => sp.UsuarioId);
            entity.HasIndex(sp => sp.Endpoint);
        });
    }
}
