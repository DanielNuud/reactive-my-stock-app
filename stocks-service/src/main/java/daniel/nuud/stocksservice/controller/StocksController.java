package daniel.nuud.stocksservice.controller;

import daniel.nuud.stocksservice.service.components.ActiveSubscription;
import daniel.nuud.stocksservice.service.WebSocketClient;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StocksController {

    private final WebSocketClient polygonClient;
    private final ActiveSubscription activeSubscription;

    @PostMapping("/subscribe/{ticker}")
    public Mono<ResponseEntity<Void>> subscribe(@PathVariable String ticker,
                                                @RequestHeader(value = "X-User-Key", required = false) @Nullable String userKey) {
        return Mono.fromRunnable(() -> {
            String user = (userKey == null || userKey.isBlank()) ? "guest" : userKey;
            activeSubscription.set(user, ticker);
            polygonClient.subscribeTo(ticker);
        }).thenReturn(ResponseEntity.accepted().build());
    }

    @PostMapping("/unsubscribe/{ticker}")
    public Mono<ResponseEntity<Void>> unsubscribe(@PathVariable String ticker,
                                                  @RequestHeader(value = "X-User-Key", required = false) @Nullable String userKey) {
        return Mono.fromRunnable(() -> {
            String user = (userKey == null || userKey.isBlank()) ? "guest" : userKey;
            activeSubscription.set(user, ticker);
            polygonClient.unsubscribe(ticker);
        }).thenReturn(ResponseEntity.accepted().build());
    }
}

