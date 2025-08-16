package daniel.nuud.companyinfoservice.repository;

import daniel.nuud.companyinfoservice.model.Company;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CompanyRepository extends ReactiveMongoRepository<Company, String> {
    Mono<Company> findByTickerIgnoreCase(String ticker);
    Mono<Boolean> existsByTickerIgnoreCase(String ticker);
}
