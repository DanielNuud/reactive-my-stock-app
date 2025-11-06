package daniel.nuud.stocksservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import daniel.nuud.stocksservice.dto.StockPriceDto;
import daniel.nuud.stocksservice.service.components.ActiveSubscription;
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
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ReactiveWebSocketServerConfig {

    private final PricesHub hub;
    private final WebSocketClient polygonClient;
    private final ActiveSubscription active;
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
            // 1) userKey берём из заголовка; если нет — "guest"
            String userKey = Optional.ofNullable(
                    session.getHandshakeInfo().getHeaders().getFirst("X-User-Key")
            ).filter(s -> !s.isBlank()).orElse("guest");

            final Sinks.Many<String> outbound = Sinks.many().unicast().onBackpressureBuffer();
            Mono<Void> send = session.send(outbound.asFlux().map(session::textMessage));
            Mono<Void> recv = session.receive().then(); // входящие не используем

            // 2) текущая активная подписка по REST
            AtomicReference<String> currentTicker = new AtomicReference<>(null);
            AtomicReference<Disposable> currentStream = new AtomicReference<>(null);

            Runnable switchTo = () -> {
                // читаем активный тикер ДЛЯ ЭТОГО userKey
                String t = active.tickerFor(userKey).orElse(null);

                // если у пользователя сейчас ничего не выбрано — остановим текущий стрим (если был)
                if (t == null) {
                    Optional.ofNullable(currentStream.getAndSet(null)).ifPresent(Disposable::dispose);
                    currentTicker.set(null);
                    return;
                }

                String norm = t.toUpperCase();
                if (norm.equals(currentTicker.get())) return; // тикер не поменялся

                // отписываем прошлый стрим, если был
                Optional.ofNullable(currentStream.getAndSet(null)).ifPresent(Disposable::dispose);
                currentTicker.set(norm);

                // подписываемся на новый тикер
                Disposable d = hub.fluxFor(norm)
                        .map(this::toJson)
                        .subscribe(json -> outbound.tryEmitNext(json));
                currentStream.set(d);

                log.info("WS[{}]: switched to ticker {}", userKey, norm);
            };

            switchTo.run();

            return Mono.when(send, recv).doFinally(sig -> {
                Optional.ofNullable(currentStream.getAndSet(null)).ifPresent(Disposable::dispose);
            });
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
