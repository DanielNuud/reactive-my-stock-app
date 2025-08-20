package daniel.nuud.stocksservice.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import daniel.nuud.stocksservice.dto.StockPriceDto;
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
public class PriceKafkaPublisher {

    private final KafkaSender<String, String> sender;
    private final ObjectMapper mapper;

    @Value("${kafka.topics.realtime}")
    private String topic;

    private final Sinks.Many<StockPriceDto> queue =
            Sinks.many().unicast().onBackpressureBuffer();

    @PostConstruct
    void start() {
        sender.send(queue.asFlux().map(this::toRecord))
                .doOnError(e -> log.warn("Kafka send error", e))
                .retry()
                .subscribe(r -> {
                    if (r.exception() != null) {
                        log.warn("Kafka nack: {}", r.exception().toString());
                    }
                });
    }

    public void publish(StockPriceDto dto) {
        var res = queue.tryEmitNext(dto);
        if (res.isFailure()) {
            log.warn("Kafka queue overflow: {}", res);
        }
    }

    private SenderRecord<String, String, Void> toRecord(StockPriceDto dto) {
        try {
            String key = dto.ticker();
            String json = mapper.writeValueAsString(dto);
            return SenderRecord.create(new ProducerRecord<>(topic, key, json), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
