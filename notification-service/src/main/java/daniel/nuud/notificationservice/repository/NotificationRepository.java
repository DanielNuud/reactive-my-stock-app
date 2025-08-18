package daniel.nuud.notificationservice.repository;

import daniel.nuud.notificationservice.model.Notification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {
    Flux<Notification> findByUserKeyOrderByCreatedAtDesc(String userKey);
    Mono<Long> countByUserKeyAndReadIsFalse(String userKey);
    Mono<Notification> findFirstByUserKeyAndDedupKey(String userKey, String dedupKey);
}
