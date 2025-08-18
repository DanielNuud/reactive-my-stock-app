package daniel.nuud.notificationservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import daniel.nuud.notificationservice.contracts.NotificationCommand;
import daniel.nuud.notificationservice.service.NotificationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final ReceiverOptions<String, String> baseOptions;
    private final ObjectMapper objectMapper;
    private final NotificationService service;

    @Value("${kafka.topics.notifications}") String topic;

    private Disposable subscription;

    @PostConstruct
    public void start() {
        var options = baseOptions.subscription(List.of(topic));

        subscription = reactor.core.publisher.Flux
                .defer(() -> KafkaReceiver.create(options).receive())
                .retryWhen(
                        Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                                .maxBackoff(Duration.ofSeconds(30))
                                .transientErrors(true)
                )
                .concatMap(rec ->
                        Mono.fromCallable(() -> objectMapper.readValue(rec.value(), NotificationCommand.class))
                                .flatMap(service::saveIdempotent)
                                .doOnNext(n -> log.info("Saved notification id={} userKey={}", n.getId(), n.getUserKey()))
                                .then(Mono.fromRunnable(rec.receiverOffset()::acknowledge))
                                .onErrorResume(ex -> {
                                    log.error("Failed to process record key={}, value={}: {}",
                                            rec.key(), rec.value(), ex.toString());
                                    return Mono.fromRunnable(rec.receiverOffset()::acknowledge);
                                })
                )
                .doOnSubscribe(s -> log.info("Kafka consumer subscribing to topic={}", topic))
                .doOnCancel(() -> log.warn("Kafka consumer subscription cancelled"))
                .doOnTerminate(() -> log.warn("Kafka consumer stream terminated"))
                .subscribe();
    }

    @PreDestroy
    public void stop() {
        if (subscription != null) subscription.dispose();
    }
}
