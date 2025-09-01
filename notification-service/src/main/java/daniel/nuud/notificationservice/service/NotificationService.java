package daniel.nuud.notificationservice.service;

import daniel.nuud.notificationservice.dto.CreateNotificationRequest;
import daniel.nuud.notificationservice.dto.NotificationResponse;
import daniel.nuud.notificationservice.model.Level;
import daniel.nuud.notificationservice.model.Notification;
import daniel.nuud.notificationservice.repository.NotificationRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final R2dbcEntityTemplate r2dbc;

    @Transactional
    public Mono<Long> createNotification(CreateNotificationRequest req) {
        log.info("Received CreateNotificationRequest {}", req);

        String level = Optional.ofNullable(req.level()).orElse("INFO").toUpperCase(Locale.ROOT);

        String sql = """
            insert into notifications as n
                (user_key, title, message, level, dedupe_key)
            values ($1, $2, $3, $4, $5)
            on conflict (dedupe_key) do update
                set dedupe_key = excluded.dedupe_key 
            returning n.id
            """;

        return r2dbc.getDatabaseClient().sql(sql)
                .bind("$1", req.userKey())
                .bind("$2", req.title())
                .bind("$3", req.message())
                .bind("$4", level)
                .bind("$5", req.dedupeKey())
                .map(row -> row.get("id", Long.class))
                .one();
    }

    public Flux<NotificationResponse> listNotifications(String userKey, @Nullable Instant since) {
        Flux<Notification> flux = (since == null)
                ? notificationRepository.findByUserKeyOrderByCreatedAtDesc(userKey).take(200)
                : notificationRepository.findByUserKeyAndCreatedAtAfterOrderByCreatedAtDesc(userKey, since);

        return flux.map(this::toResponse);
    }

    public Mono<Void> markNotificationRead(Long id) {
        return notificationRepository.findById(id)
                .flatMap(n -> {
                    n.setReadFlag(true);
                    return notificationRepository.save(n);
                })
                .then();
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUserKey(),
                n.getTitle(),
                n.getMessage(),
                n.getLevel().name(),
                n.isReadFlag(),
                n.getCreatedAt()
        );
    }
}
