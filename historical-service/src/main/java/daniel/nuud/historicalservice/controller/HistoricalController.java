package daniel.nuud.historicalservice.controller;

import daniel.nuud.historicalservice.dto.StockPrice;
import daniel.nuud.historicalservice.model.StockBar;
import daniel.nuud.historicalservice.service.HistoricalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/historical")
@RequiredArgsConstructor
public class HistoricalController {

    private final HistoricalService historicalService;

    @GetMapping("/{ticker}")
    public Flux<StockBar> getStockBar(@RequestParam String period,
                                      @PathVariable String ticker,
                                      @RequestHeader(value = "X-User-Key", defaultValue = "guest") String userKey) {
        return historicalService.getHistoricalStockBar(ticker, period, userKey);
    }

    @PostMapping("/realtime")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> receiveRealtimePrice(@RequestBody StockPrice stockPrice) {
        return Mono.fromRunnable(() -> historicalService.saveRealtimePrice(stockPrice));
    }
}
