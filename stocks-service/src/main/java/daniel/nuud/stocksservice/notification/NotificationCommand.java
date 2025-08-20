package daniel.nuud.stocksservice.notification;

public record NotificationCommand(
        String userKey,
        String title,
        String body,
        String severity,
        String code,
        long timestamp
) {}