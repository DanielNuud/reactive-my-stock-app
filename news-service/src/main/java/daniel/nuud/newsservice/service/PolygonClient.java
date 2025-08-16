package daniel.nuud.newsservice.service;

import daniel.nuud.newsservice.dto.ApiResponse;
import daniel.nuud.newsservice.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolygonClient {

    private final WebClient webClient;

    @Value("${polygon.api.key}")
    private String apiKey;

    @CircuitBreaker(name = "polygonNewsDB", fallbackMethod = "skipRefreshReactive")
    public Mono<ApiResponse> getApiResponse(String ticker) {
        String key = getString(ticker);
        return webClient.get()
                .uri("/v2/reference/news?ticker={ticker}&order=asc&limit=10&sort=published_utc&apiKey={apiKey}",
                        key, apiKey)
                .retrieve()
                .bodyToMono(ApiResponse.class)
                .filter(resp -> resp != null && resp.getResults() != null)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("News with ticker " + ticker + " not found")));
    }

    private static String getString(String query) {
        String q = query == null ? "" : query.trim().toUpperCase(Locale.ROOT);
        return q;
    }

    @SuppressWarnings("unused")
    private Mono<Boolean> skipRefreshReactive(String ticker, Throwable ex) {
        log.warn("Skip tickers refresh for '{}': {}", ticker, ex.toString());
        return Mono.just(false);
    }

}
