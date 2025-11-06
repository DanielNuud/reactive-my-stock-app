package daniel.nuud.stocksservice.service;

import daniel.nuud.stocksservice.dto.StockPriceDto;
import daniel.nuud.stocksservice.model.StockPrice;
import daniel.nuud.stocksservice.service.components.PricesHub;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
@Slf4j
@RequiredArgsConstructor
public class StocksPriceService {

    private final PricesHub pricesHub;
    private final WebClient historicalWebClient;

    private final Map<String, Deque<StockPrice>> priceMap = new ConcurrentHashMap<>();
    private static final int MAX_ENTRIES = 100;

    public void save(String ticker, double price, long timestamp) {
        save(ticker, price, timestamp, null);
    }

    public void save(String ticker, double price, long timestamp, @Nullable String targetCurrency) {
        priceMap.computeIfAbsent(ticker, t -> new ConcurrentLinkedDeque<>());
        Deque<StockPrice> deque = priceMap.get(ticker);

        if (deque.size() >= MAX_ENTRIES) {
            deque.pollFirst();
        }

        StockPrice stockPrice = new StockPrice(ticker, price, timestamp);
        deque.addLast(stockPrice);
        StockPriceDto dto = StockPriceDto.from(stockPrice);

        sendStockBar(stockPrice).doOnError(e -> log.warn("Historical post failed: {}", e.toString()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();

        log.info("EMIT {}", stockPrice.getTicker());
        pricesHub.emit(stockPrice.getTicker(), dto);

//        if (targetCurrency != null && !targetCurrency.equals("USD")) {
//            broadcastStockBar(stockPrice, targetCurrency);
//        }
    }

    public Mono<Void> sendStockBar(StockPrice  stockPrice) {
        log.info("Sending stock bar to historical: {}", stockPrice);
        return historicalWebClient.post()
                .uri("http://historical-service:8080/api/historical/realtime")
                .bodyValue(stockPrice)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                res.bodyToMono(String.class).defaultIfEmpty("")
                        .map(b -> new RuntimeException(
                                "Historical post failed: %s %s"
                                        .formatted(res.statusCode(), b)))
                )
                .toBodilessEntity()
                .then()
                .doOnError(e -> log.warn("Historical post failed: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

//    public void broadcastStockBar(StockPrice stockPrice, String targetCurrency) {
//        Double amount = stockPrice.getPrice();
//        Double convertedPrice = restClientCurrency.get()
//                .uri("http://currency-service:8080/api/currency/convert?from=USD&to={targetCurrency}&amount={amount}",
//                        targetCurrency, amount)
//                .retrieve()
//                .body(Double.class);
//
//        StockPriceDto payload = new StockPriceDto(convertedPrice, stockPrice.getTimestamp(),  stockPrice.getTicker());
//
//        messagingTemplate.convertAndSend("/topic/stocks/" + stockPrice.getTicker() + "/" + targetCurrency, payload);
//    }

}
