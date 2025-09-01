package daniel.nuud.notificationservice.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String userKey,
        String title,
        String message,
        String level,
        boolean readFlag,
        Instant createdAt
) {
}
