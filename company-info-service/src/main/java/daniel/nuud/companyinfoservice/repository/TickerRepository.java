package daniel.nuud.companyinfoservice.repository;

import daniel.nuud.companyinfoservice.model.TickerEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TickerRepository extends R2dbcRepository<TickerEntity, String> {
    Flux<TickerEntity> findTop5ByTickerStartsWith(String ticker);
}
