package daniel.nuud.notificationservice.controller;

import daniel.nuud.notificationservice.model.Notification;
import daniel.nuud.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public Flux<Notification> list(@RequestHeader(value = "X-User-Key", defaultValue = "guest") String userKey) {
        return service.listForUser(userKey);
    }

    @GetMapping("/unread-count")
    public Mono<Long> unreadCount(@RequestHeader(value = "X-User-Key", defaultValue = "guest") String userKey) {
        return service.unreadCount(userKey);
    }

    @PostMapping("/{id}/read")
    public Mono<ResponseEntity<Void>> markRead(@PathVariable String id,
                                               @RequestHeader(value = "X-User-Key", defaultValue = "guest") String userKey) {
        return service.markRead(id, userKey).thenReturn(ResponseEntity.ok().build());
    }
}
