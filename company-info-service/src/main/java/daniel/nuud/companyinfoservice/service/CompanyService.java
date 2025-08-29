package daniel.nuud.companyinfoservice.service;

import daniel.nuud.companyinfoservice.dto.ApiResponse;
import daniel.nuud.companyinfoservice.dto.Ticket;
import daniel.nuud.companyinfoservice.exception.ResourceNotFoundException;
import daniel.nuud.companyinfoservice.model.Company;
import daniel.nuud.companyinfoservice.repository.CompanyRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final R2dbcEntityTemplate template;
    private final PolygonClient polygonClient;

    @SuppressWarnings("unused")
    private Mono<Boolean> skipRefreshReactive(String ticker, Throwable ex) {
        log.warn("Skip refresh for {}: {}", ticker, ex.toString());
        return Mono.just(false);
    }

    @Bulkhead(name = "companyWrite", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "skipRefreshReactive")
    public Mono<Company> tryRefreshCompany(String ticker) {
        final String key = normalize(ticker);
        return polygonClient.getApiResponse(key)
                .map(resp -> mapToCompany(resp, key))
                .flatMap(this::upsertCompany)
                .doOnSuccess(c -> log.info("Upserted company {}", c.getTicker()));
    }

    private Mono<Company> upsertCompany(Company c) {
        final String sql = """
            insert into companies
              (ticker, description, name, homepage_url, primary_exchange, market_cap,
               city, address1, icon_url, logo_url, status)
            values ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)
            on conflict (ticker) do update set
              description=$2, name=$3, homepage_url=$4, primary_exchange=$5, market_cap=$6,
              city=$7, address1=$8, icon_url=$9, logo_url=$10, status=$11
            returning ticker, description, name, homepage_url, primary_exchange, market_cap,
                      city, address1, icon_url, logo_url, status
            """;
        return template.getDatabaseClient()
                .sql(sql)
                .bind("$1",  c.getTicker())
                .bind("$2",  c.getDescription())
                .bind("$3",  c.getName())
                .bind("$4",  c.getHomepageUrl())
                .bind("$5",  c.getPrimaryExchange())
                .bind("$6",  c.getMarketCap())
                .bind("$7",  c.getCity())
                .bind("$8",  c.getAddress1())
                .bind("$9",  c.getIconUrl())
                .bind("$10", c.getLogoUrl())
                .bind("$11", c.getStatus())
                .map((row, meta) -> template.getConverter().read(Company.class, row, meta))
                .one();
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
