package daniel.nuud.notificationservice.service;

import com.mongodb.DuplicateKeyException;
import daniel.nuud.notificationservice.contracts.NotificationCommand;
import daniel.nuud.notificationservice.model.Level;
import daniel.nuud.notificationservice.model.Notification;
import daniel.nuud.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Mono<Notification> saveIdempotent(NotificationCommand c) {
        var cmd = normalize(c);
        String dedup = cmd.dedupKey() != null ? cmd.dedupKey() : UUID.randomUUID().toString();

        return notificationRepository.findFirstByUserKeyAndDedupKey(cmd.userKey(), dedup)
                .switchIfEmpty(Mono.defer(() ->
                        notificationRepository.insert(map(cmd, dedup))
                                .onErrorResume(DuplicateKeyException.class, ex ->
                                        notificationRepository.findFirstByUserKeyAndDedupKey(cmd.userKey(), dedup))));
    }

    public Flux<Notification> listForUser(String userKey) {
        return notificationRepository.findByUserKeyOrderByCreatedAtDesc(userKey);
    }

    public Mono<Long> unreadCount(String userKey) {
        return notificationRepository.countByUserKeyAndReadIsFalse(userKey);
    }

    public Mono<Void> markRead(String id, String userKey) {
        return notificationRepository.findById(id)
                .filter(n -> n.getUserKey().equals(userKey))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Notification not found")))
                .flatMap(n -> { n.setRead(true); return notificationRepository.save(n); })
                .then();
    }


    private Notification map(NotificationCommand c, String dedupKey) {
        Notification n = new Notification();
        n.setUserKey(c.userKey());
        n.setTitle(defaultIfBlank(c.title(), "Notification"));
        n.setMessage(defaultIfBlank(c.message(), ""));
        n.setLevel(safeLevel(c.severity()));
        n.setDedupKey(dedupKey);
        n.setCreatedAt(c.createdAt() != null ? c.createdAt() : Instant.now());
        n.setRead(false);
        n.setSource(c.source());
        n.setTraceId(c.traceId());
        return n;
    }

    private static String defaultIfBlank(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    private NotificationCommand normalize(NotificationCommand c) {
        return new NotificationCommand(
                c.notificationId(),
                c.userKey() == null ? "" : c.userKey().trim(),
                c.title(),
                c.message(),
                c.severity() == null ? "INFO" : c.severity().trim().toUpperCase(Locale.ROOT),
                c.dedupKey(),
                c.createdAt(),
                c.source(),
                c.traceId()
        );
    }

    private static Level safeLevel(String s) {
        try { return Level.valueOf(s); } catch (Exception e) { return Level.INFO; }
    }
}
