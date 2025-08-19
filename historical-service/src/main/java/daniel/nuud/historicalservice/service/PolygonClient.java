package daniel.nuud.historicalservice.service;

import daniel.nuud.historicalservice.dto.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolygonClient {

    private final WebClient polygonWebClient;

    @CircuitBreaker(name = "polygonHistCB", fallbackMethod = "fallbackEmpty")
    @Retry(name = "readSafe")
    public Mono<ApiResponse> getAggregates(String ticker, String multiplier, String timespan, LocalDate from, LocalDate to, String apiKey) {

        String path = "/v2/aggs/ticker/%s/range/%s/%s/%s/%s".formatted(
                ticker.toUpperCase(), multiplier, timespan, from, to
        );

        return polygonWebClient.get()
                .uri(uri -> uri
                        .path(path)
                        .queryParam("adjusted", "true")
                        .queryParam("sort", "asc")
                        .queryParam("apiKey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(ApiResponse.class)
                .doOnError(e -> log.warn("Polygon historical error for {}: {}", ticker, e.toString()));
    }

    private Mono<ApiResponse> fallbackEmpty(String ticker, String multiplier, String timespan,
                                            LocalDate from, LocalDate to, String apiKey, Throwable ex) {
        log.warn("Polygon fallback for {} {} {} {}..{} {}: {}", ticker, multiplier, timespan, from, to, apiKey, ex.toString());
        return Mono.empty();
    }
}
