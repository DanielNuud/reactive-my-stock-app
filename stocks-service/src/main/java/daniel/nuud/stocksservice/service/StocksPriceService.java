package daniel.nuud.stocksservice.service;

import daniel.nuud.stocksservice.dto.StockPriceDto;
import daniel.nuud.stocksservice.model.StockPrice;
import daniel.nuud.stocksservice.notification.PriceKafkaPublisher;
import daniel.nuud.stocksservice.service.components.PricesHub;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
@Slf4j
@RequiredArgsConstructor
public class StocksPriceService {

    private final PricesHub pricesHub;
    private final PriceKafkaPublisher priceKafkaPublisher;

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
        pricesHub.emit(dto);
        priceKafkaPublisher.publish(dto);

//        if (targetCurrency != null && !targetCurrency.equals("USD")) {
//            broadcastStockBar(stockPrice, targetCurrency);
//        }
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
