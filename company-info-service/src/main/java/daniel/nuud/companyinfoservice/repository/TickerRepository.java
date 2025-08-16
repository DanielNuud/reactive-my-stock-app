package daniel.nuud.companyinfoservice.repository;

import daniel.nuud.companyinfoservice.model.TickerEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TickerRepository extends ReactiveMongoRepository<TickerEntity, String> {
    Flux<TickerEntity> findTop5ByTickerStartsWithIgnoreCase(String ticker);
}
