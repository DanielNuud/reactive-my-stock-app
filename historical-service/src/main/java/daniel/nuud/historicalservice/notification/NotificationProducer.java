package daniel.nuud.historicalservice.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final KafkaSender<String, String> sender;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.notifications}")
    private String topic;

    public Mono<Void> send(NotificationCommand cmd) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(cmd))
                .flatMap(json -> {
                    ProducerRecord<String, String> rec =
                            new ProducerRecord<>(topic, cmd.userKey(), json);
                    SenderRecord<String, String, Void> sr =
                            SenderRecord.create(rec, null);
                    return sender.send(Mono.just(sr)).next().then();
                })
                .doOnSuccess(v -> log.info("Notification sent, topic={}, key={}", topic, cmd.userKey()))
                .doOnError(e -> log.error("Failed to send notification", e));
    }
}
