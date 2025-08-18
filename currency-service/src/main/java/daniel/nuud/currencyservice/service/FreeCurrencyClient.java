package daniel.nuud.currencyservice.service;

import daniel.nuud.currencyservice.dto.RateResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class FreeCurrencyClient {

    private final WebClient freeCurrencyWebClient;

    @Value("${freecurrency.api.key}")
    private String apiKey;

    @CircuitBreaker(name = "fxCB", fallbackMethod = "fallbackEmpty")
    @Retry(name = "fxReadSafe")
    public Mono<RateResponse> getRates(String base) {
        String b = base == null ? "" : base.trim().toUpperCase();

        return freeCurrencyWebClient.get()
                .uri(uri -> uri.path("/v1/latest")
                        .queryParam("apikey", apiKey)
                        .queryParam("base_currency", b)
                        .build())
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) return resp.bodyToMono(RateResponse.class);
                    if (resp.statusCode().is4xxClientError()) return Mono.empty();
                    return resp.createException().flatMap(Mono::error);
                });
    }

    @SuppressWarnings("unused")
    private Mono<RateResponse> fallbackEmpty(String base, Throwable ex) {
        log.warn("FreeCurrency fallback for {}: {}", base, ex.toString());
        return Mono.empty();
    }
}
