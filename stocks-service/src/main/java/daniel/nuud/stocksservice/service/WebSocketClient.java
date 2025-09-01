package daniel.nuud.stocksservice.service;

import daniel.nuud.stocksservice.service.components.ExponentialBackoff;
import daniel.nuud.stocksservice.service.components.PolygonMessageProcessor;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketClient {

    @Value("${polygon.ws.url}")
    private String wsUrl;
    @Value("${polygon.api.key}")
    private String apiKey;

    private final PolygonMessageProcessor messageProcessor;
    private final ExponentialBackoff backoff;

    private final ReactorNettyWebSocketClient reactiveClient = new ReactorNettyWebSocketClient();
    private final Sinks.Many<String> outbound = Sinks.many().unicast().onBackpressureBuffer();
    private final AtomicReference<String> currentTicker = new AtomicReference<>(null);
    private final AtomicReference<Disposable> connectionRef = new AtomicReference<>();


    @EventListener(ApplicationReadyEvent.class)
    public void connect() {
        log.info("Application ready — opening Polygon WS");
        open();
    }

    private synchronized void ensureConnected() {
        Disposable d = connectionRef.get();
        if (d == null || d.isDisposed()) {
            log.info("No active Polygon WS connection — opening now");
            open();
        }
    }

    @PreDestroy
    public void shutdown() {
        Disposable d = connectionRef.getAndSet(null);
        if (d != null) d.dispose();
    }

    public synchronized void subscribeTo(String ticker) {
        ensureConnected();
        String t = Objects.requireNonNull(ticker).toUpperCase();
        String prev = currentTicker.getAndSet(t);
        if (prev != null && !prev.equals(t)) {
            String u = unsubJson(channel(prev));
            outbound.tryEmitNext(u);
            log.debug("Queue unsubscribe: {}", u);
        }
        String s = subJson(channel(t));
        outbound.tryEmitNext(s);
        log.info("Subscribe requested for {}", t);
        log.debug("Queue subscribe: {}", s);
    }

    public synchronized void unsubscribe(String ticker) {
        if (ticker == null) return;
        String t = ticker.toUpperCase();
        if (currentTicker.compareAndSet(t, null)) outbound.tryEmitNext(unsubJson(channel(t)));
    }

    private void open() {
        log.info("Connecting to {}", wsUrl);
        Disposable d = reactiveClient.execute(URI.create(wsUrl), this::handleSession)
                .doOnError(err -> log.warn("WS error: {}", err.toString(), err))
                .doFinally(sig -> { log.info("WS closed: {}", sig); scheduleReconnect(); })
                .subscribe();
        connectionRef.set(d);
    }

    private Mono<Void> handleSession(WebSocketSession session) {
        String auth = "{\"action\":\"auth\",\"params\":\"" + apiKey + "\"}";
        outbound.tryEmitNext(auth);
        log.debug("WS OUT queue <- {}", auth);

        Mono<Void> send = session
                .send(outbound.asFlux()
                        .doOnNext(msg -> log.debug("WS OUT -> {}", msg))
                        .map(session::textMessage))
                .doOnSubscribe(s -> log.info("WS send channel started"));

        Mono<Void> recv = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(txt -> log.debug("WS IN  <- {}", abbreviate(txt)))
                .doOnError(e -> log.warn("WS RECV error", e))
                .doOnComplete(() -> log.info("WS receive completed"))
                .doOnNext(this::onText)
                .then();

        return Mono.when(send, recv);
    }

    private String abbreviate(String s) { return (s == null || s.length() <= 400) ? s : s.substring(0, 400) + "..."; }

    private void onText(String text) {
        try {
            JSONArray arr = new JSONArray(text);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if ("status".equals(o.optString("ev"))) {
                    if ("authenticated".equalsIgnoreCase(o.optString("status"))) {
                        backoff.reset();
                        var t = currentTicker.get();
                        if (t != null) outbound.tryEmitNext(subJson(channel(t)));
                    }
                    return;
                }
            }
        } catch (Exception ignore) {

        }
        messageProcessor.process(text);
    }

    private void scheduleReconnect() {
        long delay = backoff.nextDelayMs();
        Mono.delay(Duration.ofMillis(delay)).subscribe(v -> open());
    }

    private String channel(String ticker) { return "AM." + ticker.toUpperCase(); } // агрегаты (bars)
    private String subJson(String ch)   { return "{\"action\":\"subscribe\",\"params\":\"" + ch + "\"}"; }
    private String unsubJson(String ch) { return "{\"action\":\"unsubscribe\",\"params\":\"" + ch + "\"}"; }
}
