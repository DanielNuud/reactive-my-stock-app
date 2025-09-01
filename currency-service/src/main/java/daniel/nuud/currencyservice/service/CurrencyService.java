package daniel.nuud.currencyservice.service;

import daniel.nuud.currencyservice.exception.ResourceNotFoundException;
import daniel.nuud.currencyservice.notification.NotificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

    private final RatesProvider ratesProvider;
    private final NotificationClient  notificationClient;

    public Mono<Double> convert(String from, String to, Double amount, String userKey) {
        final String base = normalize(from);
        final String quote = normalize(to);

        if (base.equals(quote)) {
            return notifySuccess(userKey, base, quote, amount, amount)
                    .onErrorResume(e -> {
                        log.warn("notifySuccess failed (same-currency): {}", e.toString());
                        return Mono.empty();
                    })
                    .thenReturn(amount);
        }

        return ratesProvider.getRates(base)
                .flatMap(map -> pickRate(map, quote))
                .map(rateStr -> multiply(amount, rateStr))
                .flatMap(result ->
                        notifySuccess(userKey, base, quote, amount, result)
                                .onErrorResume(e -> {
                                    log.warn("notifySuccess failed: {}", e.toString());
                                    return Mono.empty();
                                })
                                .thenReturn(result)
                )
                .onErrorResume(ex ->
                        notifyFailure(userKey)
                                .onErrorResume(e -> {
                                    log.warn("notifyFailure failed: {}", e.toString());
                                    return Mono.empty();
                                })
                                .then(Mono.error(ex))
                );
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private Mono<String> pickRate(Map<String, String> rates, String quote) {
        String r = rates.get(quote);
        if (r == null) {
            return Mono.error(new ResourceNotFoundException("Rate " + quote + " not found"));
        }
        return Mono.just(r);
    }

    private Double multiply(Double amount, String rateStr) {
        return new BigDecimal(amount.toString())
                .multiply(new BigDecimal(rateStr))
                .doubleValue();
    }

    private Mono<Void> notifySuccess(String userKey, String from, String to, Double amount, Double result) {
           return notificationClient.sendNotification(
                    userKey,
                    "Conversion completed",
                    amount + " " + from + " → " + String.format("%.4f", result) + " " + to,
                    "INFO",
                    "FX:CONVERT:" + from + ":" + to + ":" + amount);
    }

    private Mono<Void> notifyFailure(String userKey) {
        return notificationClient.sendNotification(
                userKey,
                "Conversion failed",
                "Please check your currency rates and try again.",
                "ERROR",
                "FX:INVALID:" + epochMinute());
    }

    private Long epochMinute() {
        return Instant.now().getEpochSecond() / 60;
    }
}
