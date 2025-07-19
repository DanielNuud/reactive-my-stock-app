package daniel.nuud.companyinfoservice.service;

import daniel.nuud.companyinfoservice.dto.ApiResponse;
import daniel.nuud.companyinfoservice.dto.Ticket;
import daniel.nuud.companyinfoservice.exception.ResourceNotFoundException;
import daniel.nuud.companyinfoservice.model.Company;
import daniel.nuud.companyinfoservice.repository.CompanyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private WebClient webClient;

    @Value("${polygon.api.key}")
    private String apiKey;

    public Mono<Company> fetchCompany(String ticker) {
        log.info(">> fetchCompany called for {}", ticker);

        return companyRepository.findByTickerIgnoreCase(ticker)
                .doOnNext(c -> log.info("Company {} found in database", c.getTicker()))
                .switchIfEmpty(
                        callPolygonApi(ticker)
                                .flatMap(this::getCompany)
                );
    }

    private Mono<ApiResponse> callPolygonApi(String ticker) {
        return webClient.get()
                .uri("/v3/reference/tickers/{ticker}?apiKey={apiKey}", ticker.toUpperCase(), apiKey)
                .retrieve()
                .bodyToMono(ApiResponse.class)
                .filter(resp -> resp != null && resp.getResults() != null)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Company with ticker " + ticker + " not found")));
    }

    private Mono<Company> getCompany(ApiResponse response) {
        Ticket data = response.getResults();

        Company company = new Company();
        company.setTicker(defaultIfNull(data.getTicker(), "Not found"));
        company.setName(defaultIfNull(data.getName(), "Not found"));
        company.setDescription(defaultIfNull(data.getDescription(), "Not found"));
        company.setHomepageUrl(defaultIfNull(data.getHomepageUrl(), "Not found"));
        company.setCity(data.getAddress() != null ? defaultIfNull(data.getAddress().getCity(), "Not found") : "Not found");
        company.setAddress1(data.getAddress() != null ? defaultIfNull(data.getAddress().getAddress1(), "Not found") : "Not found");
        company.setLogoUrl(data.getBranding() != null ? defaultIfNull(data.getBranding().getLogoUrl(), "Not found") : "Not found");
        company.setIconUrl(data.getBranding() != null ? defaultIfNull(data.getBranding().getIconUrl(), "Not found") : "Not found");
        company.setMarketCap(defaultIfNull(String.valueOf(data.getMarketCap()), "Not found"));
        company.setPrimaryExchange(defaultIfNull(data.getPrimaryExchange(), "Not found"));
        company.setStatus(defaultIfNull(response.getStatus(), "Not found"));

        return companyRepository.save(company);
    }

    private String defaultIfNull(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}
