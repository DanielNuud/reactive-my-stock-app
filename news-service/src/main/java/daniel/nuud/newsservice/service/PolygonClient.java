package daniel.nuud.newsservice.service;

import daniel.nuud.newsservice.dto.ApiResponse;
import daniel.nuud.newsservice.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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
    @Retry(name = "readSafe")
    public Mono<ApiResponse> getApiResponse(String ticker) {
        return webClient.get()
                .uri("/v2/reference/news?ticker={ticker}&order=asc&limit=10&sort=published_utc&apiKey={apiKey}",
                        ticker, apiKey)
                .retrieve()
                .bodyToMono(ApiResponse.class);
    }

    @SuppressWarnings("unused")
    private Mono<Boolean> skipRefreshReactive(String ticker, Throwable ex) {
        log.warn("Skip tickers refresh for '{}': {}", ticker, ex.toString());
        return Mono.just(false);
    }

}
