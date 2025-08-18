package daniel.nuud.currencyservice.notification;

import java.time.Instant;

public record NotificationCommand(
        String notificationId,
        String userKey,
        String title,
        String message,
        String severity,
        String dedupKey,
        Instant createdAt,
        String source,
        String traceId
) {}
