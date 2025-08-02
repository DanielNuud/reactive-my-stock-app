package daniel.nuud.stocksservice.service;

import daniel.nuud.stocksservice.model.StockPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
@Slf4j
public class StocksPriceService {

    private final Map<String, Deque<StockPrice>> priceMap = new ConcurrentHashMap<>();
    private static final int MAX_ENTRIES = 100;

    public void save(StockPrice price) {
        priceMap.computeIfAbsent(price.getTicker(), t -> new ConcurrentLinkedDeque<>());
        Deque<StockPrice> deque = priceMap.get(price.getTicker());

        if (deque.size() >= MAX_ENTRIES) {
            deque.pollFirst();
        }
        deque.addLast(price);
    }

    public List<StockPrice> getPrices(String ticker) {
        return new ArrayList<>(priceMap.getOrDefault(ticker, new LinkedList<>()));
    }

    public Set<String> getTickers() {
        return priceMap.keySet();
    }
}
