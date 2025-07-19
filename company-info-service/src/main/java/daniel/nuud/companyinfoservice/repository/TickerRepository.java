package daniel.nuud.companyinfoservice.repository;

import daniel.nuud.companyinfoservice.model.Ticker;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TickerRepository extends ReactiveMongoRepository<Ticker, String> {
    Flux<Ticker> findTop5ByTickerStartsWithIgnoreCase(String ticker);
}
