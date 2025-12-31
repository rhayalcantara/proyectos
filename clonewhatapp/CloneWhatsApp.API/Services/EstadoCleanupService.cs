using Microsoft.EntityFrameworkCore;
using CloneWhatsApp.API.Data;
using CloneWhatsApp.API.Models;

namespace CloneWhatsApp.API.Services;

public class EstadoCleanupService : BackgroundService
{
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<EstadoCleanupService> _logger;
    private readonly TimeSpan _interval = TimeSpan.FromHours(1);

    public EstadoCleanupService(IServiceProvider serviceProvider, ILogger<EstadoCleanupService> logger)
    {
        _serviceProvider = serviceProvider;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("EstadoCleanupService iniciado");

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await CleanupExpiredStatuses();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error al limpiar estados expirados");
            }

            await Task.Delay(_interval, stoppingToken);
        }
    }

    private async Task CleanupExpiredStatuses()
    {
        using var scope = _serviceProvider.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<ApplicationDbContext>();

        var now = DateTime.UtcNow;

        // Obtener estados expirados
        var estadosExpirados = await context.Estados
            .Where(e => e.FechaExpiracion < now)
            .ToListAsync();

        if (estadosExpirados.Count == 0)
        {
            _logger.LogDebug("No hay estados expirados para eliminar");
            return;
        }

        // Eliminar archivos de imagen
        foreach (var estado in estadosExpirados.Where(e => e.Tipo == TipoEstado.Imagen && !string.IsNullOrEmpty(e.UrlArchivo)))
        {
            try
            {
                var filePath = Path.Combine(Directory.GetCurrentDirectory(), estado.UrlArchivo!.TrimStart('/'));
                if (File.Exists(filePath))
                {
                    File.Delete(filePath);
                    _logger.LogDebug("Archivo eliminado: {FilePath}", filePath);
                }
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "No se pudo eliminar archivo: {UrlArchivo}", estado.UrlArchivo);
            }
        }

        // Eliminar estados de la base de datos
        context.Estados.RemoveRange(estadosExpirados);
        var deleted = await context.SaveChangesAsync();

        _logger.LogInformation("Se eliminaron {Count} estados expirados", deleted);
    }
}
