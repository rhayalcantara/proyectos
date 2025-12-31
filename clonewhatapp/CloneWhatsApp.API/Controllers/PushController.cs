using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using CloneWhatsApp.API.DTOs;
using CloneWhatsApp.API.Services;

namespace CloneWhatsApp.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class PushController : ControllerBase
{
    private readonly IPushNotificationService _pushService;

    public PushController(IPushNotificationService pushService)
    {
        _pushService = pushService;
    }

    private Guid GetCurrentUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.Parse(userIdClaim!);
    }

    [HttpGet("vapid-public-key")]
    public ActionResult<VapidPublicKeyDTO> GetVapidPublicKey()
    {
        var publicKey = _pushService.GetVapidPublicKey();
        return Ok(new VapidPublicKeyDTO { PublicKey = publicKey });
    }

    [HttpPost("subscribe")]
    [Authorize]
    public async Task<ActionResult> Subscribe([FromBody] SuscripcionPushDTO dto)
    {
        var userId = GetCurrentUserId();
        await _pushService.SubscribeAsync(userId, dto);
        return Ok(new { message = "Suscripción registrada correctamente" });
    }

    [HttpPost("unsubscribe")]
    [Authorize]
    public async Task<ActionResult> Unsubscribe([FromBody] UnsubscribeDTO dto)
    {
        var userId = GetCurrentUserId();
        await _pushService.UnsubscribeAsync(userId, dto.Endpoint);
        return Ok(new { message = "Suscripción cancelada correctamente" });
    }
}

public class UnsubscribeDTO
{
    public string Endpoint { get; set; } = string.Empty;
}
