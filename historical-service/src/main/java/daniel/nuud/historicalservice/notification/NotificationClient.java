package daniel.nuud.historicalservice.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final WebClient notificationWebClient;

    public Mono<Void> sendNotification(String userKey, String title, String message, String level,
                                       String dedupeSuffix) {

        String dedupeKey = buildDedupeKey(userKey, dedupeSuffix);

        var body = Map.of(
                "userKey", userKey,
                "title", title,
                "message", message,
                "level", level,
                "dedupeKey", dedupeKey
        );

        return notificationWebClient.post()
                .uri("/api/notifications")
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class).defaultIfEmpty("")
                                .map(b -> new RuntimeException(
                                        "Notify failed: %s %s".formatted(res.statusCode(), b)))
                )
                .toBodilessEntity()
                .then()
                .doOnError(e -> log.warn("Notification post failed: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    private String buildDedupeKey(String userKey, String suffix) {
        long epochMinute = Instant.now().getEpochSecond() / 60;
        return userKey + ":" + suffix + ":" + epochMinute;
    }
}
