package daniel.nuud.historicalservice.controller;

import daniel.nuud.historicalservice.model.StockBar;
import daniel.nuud.historicalservice.service.HistoricalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/historical")
@RequiredArgsConstructor
public class HistoricalController {

    private final HistoricalService historicalService;

    @GetMapping("/{ticker}")
    public Flux<StockBar> getStockBar(
            @RequestParam String period,
            @RequestParam Integer multiplier,
            @RequestParam String timespan,
            @PathVariable String ticker) {
        return historicalService.getHistoricalStockBar(ticker, period, multiplier, timespan);
    }
}
