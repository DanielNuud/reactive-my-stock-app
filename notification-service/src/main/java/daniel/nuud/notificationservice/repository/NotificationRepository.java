package daniel.nuud.notificationservice.repository;

import daniel.nuud.notificationservice.model.Notification;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface NotificationRepository extends R2dbcRepository<Notification, Long> {

    @Query("""
     SELECT id, user_key, title, message, level, read_flag, created_at
     FROM notifications
     WHERE user_key = :userKey
     ORDER BY created_at DESC
     LIMIT :limit
  """)
    Flux<Notification> findTopByUserKey(String userKey, int limit);

    @Query("""
     SELECT id, user_key, title, message, level, read_flag, created_at
     FROM notifications
     WHERE user_key = :userKey AND created_at > :since
     ORDER BY created_at DESC
     LIMIT :limit
  """)
    Flux<Notification> findTopByUserKeySince(String userKey, Instant since, int limit);
}