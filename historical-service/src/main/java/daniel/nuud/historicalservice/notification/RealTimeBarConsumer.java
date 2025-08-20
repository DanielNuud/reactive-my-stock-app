package daniel.nuud.historicalservice.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import daniel.nuud.historicalservice.dto.StockPrice;
import daniel.nuud.historicalservice.service.HistoricalService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealTimeBarConsumer {

    private final ReceiverOptions<String, String> baseOptions;
    private final ObjectMapper mapper;
    private final HistoricalService historical;

    @Value("${kafka.topics.realtime}")
    private String topic;

    @PostConstruct
    public void subscribe() {
        KafkaReceiver.create(baseOptions.subscription(List.of(topic)))
                .receive()
                .concatMap(rec ->
                        Mono.fromCallable(() -> mapper.readValue(rec.value(), StockPrice.class))
                                .map(dto -> new StockPrice(dto.getTicker(), dto.getPrice(), dto.getTimestamp()))
                                .doOnNext(historical::saveRealtimePrice)
                                .then(Mono.fromRunnable(rec.receiverOffset()::acknowledge))
                                .onErrorResume(e -> {
                                    log.warn("Bad message, skip: {}", rec.value(), e);
                                    rec.receiverOffset().acknowledge();
                                    return Mono.empty();
                                })
                )
                .doOnSubscribe(s -> log.info("Kafka consumer subscribed to {}", topic))
                .subscribe();
    }
}
