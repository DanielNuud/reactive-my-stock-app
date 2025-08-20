package daniel.nuud.stocksservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import daniel.nuud.stocksservice.dto.StockPriceDto;
import daniel.nuud.stocksservice.service.components.PricesHub;
import daniel.nuud.stocksservice.service.WebSocketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ReactiveWebSocketServerConfig {

    private final PricesHub hub;
    private final WebSocketClient polygonClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Bean
    public HandlerMapping wsMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(-1);
        mapping.setUrlMap(Map.of("/ws/prices", pricesHandler()));
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    @Bean
    public WebSocketHandler pricesHandler() {
        return session -> {

            var outbound = hub.stream()
                    .map(this::toJson)
                    .map(session::textMessage);

            Mono<Void> inbound = session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(txt -> {
                        try {
                            var node = mapper.readTree(txt);
                            var action = node.path("action").asText("");
                            var ticker = node.path("ticker").asText("");
                            if ("subscribe".equalsIgnoreCase(action) && !ticker.isBlank()) {
                                polygonClient.subscribeTo(ticker);
                                log.info("WS client requested subscribe: {}", ticker);
                            } else if ("unsubscribe".equalsIgnoreCase(action) && !ticker.isBlank()) {
                                polygonClient.unsubscribe(ticker);
                                log.info("WS client requested unsubscribe: {}", ticker);
                            }
                        } catch (Exception e) {
                            log.warn("Bad WS command: {}", txt, e);
                        }
                    })
                    .then();

            return session.send(outbound).and(inbound);
        };
    }

    private String toJson(StockPriceDto dto) {
        try {
            return mapper.writeValueAsString(dto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
