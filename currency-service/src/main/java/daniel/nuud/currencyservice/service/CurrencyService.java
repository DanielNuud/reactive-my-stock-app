package daniel.nuud.currencyservice.service;

import daniel.nuud.currencyservice.dto.RateResponse;
import daniel.nuud.currencyservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final WebClient webClient;

    @Value("${freecurrency.api.key}")
    private String apiKey;

    @Cacheable(cacheNames = "currency", key = "#currency")
    public Mono<Map<String, String>> getCurrencyRates(String currency) {
        return webClient.get()
                .uri("/v1/latest?apikey={apiKey}&base_currency={currency}", apiKey, currency)
                .retrieve()
                .bodyToMono(RateResponse.class)
                .flatMap(response -> {
                    if (response == null || response.getRates() == null) {
                        return Mono.error(new ResourceNotFoundException("Currency \"" + currency + "\" not found"));
                    }
                    return Mono.just(response.getRates());
                });
    }


    public Mono<Double> convert(String fromCurrency, String toCurrency, Double amount) {
        return getCurrencyRates(fromCurrency)
                .map(rates -> {
                    String rateString = rates.get(toCurrency);
                    if (rateString == null) {
                        throw new ResourceNotFoundException("Currency \"" + toCurrency + "\" not found");
                    }
                    double rate = Double.parseDouble(rateString);
                    return amount * rate;
                });
    }
}
