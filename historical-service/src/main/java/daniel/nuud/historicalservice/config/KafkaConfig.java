package daniel.nuud.historicalservice.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    @Value("${kafka.bootstrapServers}")
    private String bootstrap;

    @Bean
    public KafkaSender<String, String> kafkaSender() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        cfg.put(ProducerConfig.CLIENT_ID_CONFIG, "historical-service-" + UUID.randomUUID());
        cfg.put(ProducerConfig.ACKS_CONFIG, "1");
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return KafkaSender.create(SenderOptions.create(cfg));
    }
}
