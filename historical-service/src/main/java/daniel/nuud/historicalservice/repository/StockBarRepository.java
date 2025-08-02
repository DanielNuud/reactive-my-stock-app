package daniel.nuud.historicalservice.repository;

import daniel.nuud.historicalservice.model.StockBar;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface StockBarRepository extends ReactiveMongoRepository<StockBar, String> {
    Flux<StockBar> findAllByTimestampBetween(Long start, Long end);
    Flux<StockBar> findByTickerAndDateBetween(String ticker, String fromDate, String toDate);
}
