package daniel.nuud.companyinfoservice.repository;

import daniel.nuud.companyinfoservice.model.Company;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CompanyRepository extends R2dbcRepository<Company, String> {
    Mono<Company> findByTicker(String ticker);
    Mono<Boolean> existsByTickerIgnoreCase(String ticker);
}
