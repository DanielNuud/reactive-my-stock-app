package daniel.nuud.companyinfoservice.service;

import daniel.nuud.companyinfoservice.dto.ApiResponse;
import daniel.nuud.companyinfoservice.dto.TickerApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolygonClient {

    private final WebClient polygonWebClient;

    @Value("${polygon.api.key}")
    private String apiKey;

    @CircuitBreaker(name = "polygonCompanyCB", fallbackMethod = "fallbackEmpty")
    @Retry(name = "readSafe")
    public Mono<ApiResponse> getApiResponse(String ticker) {
        log.info(">>> fetchCompany called for {}", ticker);
        String t = getString(ticker);

        return polygonWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/reference/tickers/{ticker}")
                        .queryParam("apiKey", apiKey)
                        .build(t))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new IllegalStateException(
                                        "Polygon error " + resp.statusCode() + " " + body))))
                .bodyToMono(ApiResponse.class);
    }

    @CircuitBreaker(name = "polygonCompanyCB", fallbackMethod = "fallbackEmptyTickers")
    @Retry(name = "readSafe")
    public Mono<TickerApiResponse> searchTickers(String query) {
        String q = getString(query);

        return polygonWebClient.get()
                .uri(uri -> uri.path("/v3/reference/tickers")
                        .queryParam("market", "stocks")
                        .queryParam("search", q)
                        .queryParam("apiKey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(TickerApiResponse.class);
    }

    private static String getString(String query) {
        String q = query == null ? "" : query.trim().toUpperCase(Locale.ROOT);
        return q;
    }

    @SuppressWarnings("unused")
    private Mono<TickerApiResponse> fallbackEmptyTickers(String query, Throwable ex) {
        log.warn("Polygon fallback for search '{}': {}", query, ex.toString());
        return Mono.empty();
    }

    @SuppressWarnings("unused")
    private Mono<ApiResponse> fallbackEmpty(String ticker, Throwable ex) {
        log.warn("Polygon fallback for company {}: {}", ticker, ex.toString());
        return Mono.empty();
    }

}
