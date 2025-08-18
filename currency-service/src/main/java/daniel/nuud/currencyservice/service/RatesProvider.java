package daniel.nuud.currencyservice.service;

import daniel.nuud.currencyservice.dto.RateResponse;
import daniel.nuud.currencyservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RatesProvider {

    private final FreeCurrencyClient client;

    @Cacheable(value = "fxRates", key = "#base.toUpperCase()", sync = true)
    public Mono<Map<String, String>> getRates(String base) {
        String b = base == null ? "" : base.trim().toUpperCase();

        return client.getRates(b)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Currency \"" + b + "\" not found")))
                .map(RateResponse::getRates)
                .filter(r -> r != null && !r.isEmpty())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Currency rates for \"" + b + "\" not found")));
    }
}
