package daniel.nuud.currencyservice.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaSender<String, String> sender;
    private final ObjectMapper om;
    @Value("${kafka.topics.notifications}") String topic;

    public Mono<Void> send(NotificationCommand cmd) {
        return Mono.fromCallable(() -> om.writeValueAsString(cmd))
                .map(json -> SenderRecord.create(new ProducerRecord<>(topic, cmd.userKey(), json), cmd.notificationId()))
                .as(sender::send)
                .then();
    }
}
