package daniel.nuud.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateNotificationRequest(
        @NotBlank String userKey,
        @NotBlank String title,
        @NotBlank String message,
        String level,
        @NotBlank String dedupeKey
) {
}
