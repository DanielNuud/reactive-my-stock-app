package daniel.nuud.notificationservice.controller;

import daniel.nuud.notificationservice.dto.CreateNotificationRequest;
import daniel.nuud.notificationservice.dto.NotificationResponse;
import daniel.nuud.notificationservice.model.Notification;
import daniel.nuud.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> createNotification(
            @RequestBody @Valid CreateNotificationRequest request) {

        return notificationService.createNotification(request)
                .map(id -> ResponseEntity.accepted().body(Map.of("id", id)));
    }

    @GetMapping
    public Flux<NotificationResponse> getNotifications(
            @RequestParam String userKey,
            @RequestParam(required = false) Instant since) {

        return notificationService.listNotifications(userKey, since);
    }

    @PatchMapping("/{id}/read")
    public Mono<ResponseEntity<Void>> markRead(@PathVariable Long id) {
        return notificationService.markNotificationRead(id)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
