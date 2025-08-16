package daniel.nuud.companyinfoservice.service;

import daniel.nuud.companyinfoservice.dto.ApiResponse;
import daniel.nuud.companyinfoservice.dto.Ticket;
import daniel.nuud.companyinfoservice.exception.ResourceNotFoundException;
import daniel.nuud.companyinfoservice.model.Company;
import daniel.nuud.companyinfoservice.repository.CompanyRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

    @Autowired
    private final CompanyRepository companyRepository;

    private final PolygonClient polygonClient;

    @SuppressWarnings("unused")
    private Mono<Boolean> skipRefreshReactive(String ticker, Throwable ex) {
        log.warn("Skip refresh for {}: {}", ticker, ex.toString());
        return Mono.just(false);
    }

    @Bulkhead(name = "companyWrite", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "skipRefreshReactive")
    public Mono<Boolean> tryRefreshCompany(String ticker) {
        final String key = normalize(ticker);
        return Mono.just(ticker)
                .filterWhen(t -> companyRepository.existsByTickerIgnoreCase(t).map(exists -> !exists))
                .flatMap(polygonClient::getApiResponse)
                .flatMap(resp -> Mono.justOrEmpty(resp.getResults())
                        .map(r -> mapToCompany(resp, key))
                        .flatMap(companyRepository::save)
                        .thenReturn(true)
                )
                .defaultIfEmpty(false)
                .onErrorReturn(false);
    }

    @Cacheable(value = "tickerSuggest", key = "#ticker", sync = true)
    @Bulkhead(name = "companyRead", type = Bulkhead.Type.SEMAPHORE)
    public Mono<Company> getFromDB(String ticker) {
        final String key = normalize(ticker);
        return companyRepository.findByTickerIgnoreCase(key)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Company with " + ticker + " not found")));
    }

    private Company mapToCompany(ApiResponse response, String key) {
        Ticket data = response.getResults();

        Company c = new Company();

        c.setTicker(key);
        c.setName(defaultIfNull(data.getName(), "Not found"));
        c.setDescription(defaultIfNull(data.getDescription(), "Not found"));
        c.setHomepageUrl(defaultIfNull(data.getHomepageUrl(), "Not found"));

        if (data.getAddress() != null) {
            c.setCity(defaultIfNull(data.getAddress().getCity(), "Not found"));
            c.setAddress1(defaultIfNull(data.getAddress().getAddress1(), "Not found"));
        }

        if (data.getBranding() != null) {
            c.setLogoUrl(defaultIfNull(data.getBranding().getLogoUrl(), "Not found"));
            c.setIconUrl(defaultIfNull(data.getBranding().getIconUrl(), "Not found"));
        }

        c.setMarketCap(defaultIfNull(String.valueOf(data.getMarketCap()), "Not found"));
        c.setPrimaryExchange(defaultIfNull(data.getPrimaryExchange(), "Not found"));
        c.setStatus(defaultIfNull(response.getStatus(), "Not found"));

        return c;
    }

    private String defaultIfNull(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    private static String normalize(String t) {
        return t == null ? "" : t.trim().toUpperCase(Locale.ROOT);
    }
}
