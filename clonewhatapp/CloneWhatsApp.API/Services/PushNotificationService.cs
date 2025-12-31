using System.Text.Json;
using CloneWhatsApp.API.Data;
using CloneWhatsApp.API.DTOs;
using CloneWhatsApp.API.Models;
using Microsoft.EntityFrameworkCore;
using WebPush;

namespace CloneWhatsApp.API.Services;

public interface IPushNotificationService
{
    Task<SuscripcionPush> SubscribeAsync(Guid usuarioId, SuscripcionPushDTO suscripcion);
    Task UnsubscribeAsync(Guid usuarioId, string endpoint);
    Task SendNotificationAsync(Guid usuarioId, PushNotificationPayloadDTO payload);
    Task SendNotificationToManyAsync(IEnumerable<Guid> usuarioIds, PushNotificationPayloadDTO payload);
    string GetVapidPublicKey();
}

public class PushNotificationService : IPushNotificationService
{
    private readonly ApplicationDbContext _context;
    private readonly IConfiguration _configuration;
    private readonly ILogger<PushNotificationService> _logger;
    private readonly WebPushClient _webPushClient;
    private readonly VapidDetails _vapidDetails;

    public PushNotificationService(
        ApplicationDbContext context,
        IConfiguration configuration,
        ILogger<PushNotificationService> logger)
    {
        _context = context;
        _configuration = configuration;
        _logger = logger;
        _webPushClient = new WebPushClient();

        var subject = _configuration["Vapid:Subject"] ?? "mailto:admin@localhost";
        var publicKey = _configuration["Vapid:PublicKey"] ?? throw new InvalidOperationException("VAPID PublicKey not configured");
        var privateKey = _configuration["Vapid:PrivateKey"] ?? throw new InvalidOperationException("VAPID PrivateKey not configured");

        _vapidDetails = new VapidDetails(subject, publicKey, privateKey);
    }

    public string GetVapidPublicKey()
    {
        return _configuration["Vapid:PublicKey"] ?? string.Empty;
    }

    public async Task<SuscripcionPush> SubscribeAsync(Guid usuarioId, SuscripcionPushDTO suscripcion)
    {
        // Verificar si ya existe una suscripción con el mismo endpoint
        var existente = await _context.SuscripcionesPush
            .FirstOrDefaultAsync(s => s.UsuarioId == usuarioId && s.Endpoint == suscripcion.Endpoint);

        if (existente != null)
        {
            // Actualizar la suscripción existente
            existente.P256dh = suscripcion.Keys.P256dh;
            existente.Auth = suscripcion.Keys.Auth;
            existente.Activo = true;
            existente.FechaCreacion = DateTime.UtcNow;
        }
        else
        {
            // Crear nueva suscripción
            existente = new SuscripcionPush
            {
                Id = Guid.NewGuid(),
                UsuarioId = usuarioId,
                Endpoint = suscripcion.Endpoint,
                P256dh = suscripcion.Keys.P256dh,
                Auth = suscripcion.Keys.Auth,
                Activo = true,
                FechaCreacion = DateTime.UtcNow
            };
            _context.SuscripcionesPush.Add(existente);
        }

        await _context.SaveChangesAsync();
        return existente;
    }

    public async Task UnsubscribeAsync(Guid usuarioId, string endpoint)
    {
        var suscripcion = await _context.SuscripcionesPush
            .FirstOrDefaultAsync(s => s.UsuarioId == usuarioId && s.Endpoint == endpoint);

        if (suscripcion != null)
        {
            _context.SuscripcionesPush.Remove(suscripcion);
            await _context.SaveChangesAsync();
        }
    }

    public async Task SendNotificationAsync(Guid usuarioId, PushNotificationPayloadDTO payload)
    {
        var suscripciones = await _context.SuscripcionesPush
            .Where(s => s.UsuarioId == usuarioId && s.Activo)
            .ToListAsync();

        foreach (var suscripcion in suscripciones)
        {
            await SendPushAsync(suscripcion, payload);
        }
    }

    public async Task SendNotificationToManyAsync(IEnumerable<Guid> usuarioIds, PushNotificationPayloadDTO payload)
    {
        var suscripciones = await _context.SuscripcionesPush
            .Where(s => usuarioIds.Contains(s.UsuarioId) && s.Activo)
            .ToListAsync();

        var tasks = suscripciones.Select(s => SendPushAsync(s, payload));
        await Task.WhenAll(tasks);
    }

    private async Task SendPushAsync(SuscripcionPush suscripcion, PushNotificationPayloadDTO payload)
    {
        try
        {
            var pushSubscription = new PushSubscription(
                suscripcion.Endpoint,
                suscripcion.P256dh,
                suscripcion.Auth
            );

            var payloadJson = JsonSerializer.Serialize(payload, new JsonSerializerOptions
            {
                PropertyNamingPolicy = JsonNamingPolicy.CamelCase
            });

            await _webPushClient.SendNotificationAsync(pushSubscription, payloadJson, _vapidDetails);
            _logger.LogInformation("Push notification sent to user {UserId}", suscripcion.UsuarioId);
        }
        catch (WebPushException ex) when (ex.StatusCode == System.Net.HttpStatusCode.Gone ||
                                          ex.StatusCode == System.Net.HttpStatusCode.NotFound)
        {
            // La suscripción ya no es válida, marcarla como inactiva
            _logger.LogWarning("Push subscription expired for user {UserId}, marking as inactive", suscripcion.UsuarioId);
            suscripcion.Activo = false;
            await _context.SaveChangesAsync();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending push notification to user {UserId}", suscripcion.UsuarioId);
        }
    }
}
