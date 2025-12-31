using System.ComponentModel.DataAnnotations;

namespace CloneWhatsApp.API.DTOs;

public class SuscripcionPushDTO
{
    [Required]
    public string Endpoint { get; set; } = string.Empty;

    [Required]
    public PushKeysDTO Keys { get; set; } = null!;
}

public class PushKeysDTO
{
    [Required]
    public string P256dh { get; set; } = string.Empty;

    [Required]
    public string Auth { get; set; } = string.Empty;
}

public class VapidPublicKeyDTO
{
    public string PublicKey { get; set; } = string.Empty;
}

public class PushNotificationPayloadDTO
{
    public string Title { get; set; } = string.Empty;
    public string Body { get; set; } = string.Empty;
    public string? Icon { get; set; }
    public string? Badge { get; set; }
    public string? Tag { get; set; }
    public PushDataDTO? Data { get; set; }
}

public class PushDataDTO
{
    public string? ChatId { get; set; }
    public string? Type { get; set; }
    public string? Url { get; set; }
}
