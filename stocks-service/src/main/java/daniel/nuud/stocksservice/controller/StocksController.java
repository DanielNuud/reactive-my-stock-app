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
                                                @RequestHeader(value="X-User-Key", required=false) String userKey) {
        return Mono.fromRunnable(() -> {
            boolean first = activeSubscription.subscribe(userKey, ticker);
            if (first) polygonClient.subscribeTo(ticker);   // 0 -> 1
        }).thenReturn(ResponseEntity.accepted().build());
    }

    @PostMapping("/unsubscribe/{ticker}")
    public Mono<ResponseEntity<Void>> unsubscribe(@PathVariable String ticker,
                                                  @RequestHeader(value="X-User-Key", required=false) String userKey) {
        return Mono.fromRunnable(() -> {
            boolean last = activeSubscription.unsubscribe(userKey, ticker);
            if (last) polygonClient.unsubscribe(ticker); // 1 -> 0
        }).thenReturn(ResponseEntity.accepted().build());
    }
}

