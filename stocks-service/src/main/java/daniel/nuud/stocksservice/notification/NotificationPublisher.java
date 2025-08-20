package daniel.nuud.stocksservice.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final KafkaSender<String, String> sender;
    private final ObjectMapper mapper;

    @Value("${kafka.topics.notifications}")
    private String topic;

    private final Sinks.Many<NotificationCommand> queue =
            Sinks.many().unicast().onBackpressureBuffer();

    @PostConstruct
    void start() {
        sender.send(queue.asFlux().map(this::toRecord))
                .doOnError(e -> log.warn("Kafka notification send error", e))
                .subscribe(r -> {
                    if (r.exception() != null) {
                        log.warn("Kafka notification nack: {}", r.exception().toString());
                    }
                });
    }

    public void publish(NotificationCommand cmd) {
        var res = queue.tryEmitNext(cmd);
        if (res.isFailure()) log.warn("Notification queue overflow: {}", res);
    }

    private SenderRecord<String, String, Void> toRecord(NotificationCommand cmd) {
        try {
            String key = cmd.userKey();
            String json = mapper.writeValueAsString(cmd);
            return SenderRecord.create(new ProducerRecord<>(topic, key, json), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
