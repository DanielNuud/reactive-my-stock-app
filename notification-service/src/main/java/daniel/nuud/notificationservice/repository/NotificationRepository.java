package daniel.nuud.notificationservice.repository;

import daniel.nuud.notificationservice.model.Notification;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface NotificationRepository extends R2dbcRepository<Notification, Long> {
    Flux<Notification> findByUserKeyOrderByCreatedAtDesc(String userKey);
    Flux<Notification> findByUserKeyAndCreatedAtAfterOrderByCreatedAtDesc(String userKey, Instant since);
}
