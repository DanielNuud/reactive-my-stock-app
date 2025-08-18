package daniel.nuud.currencyservice.service;

import daniel.nuud.currencyservice.dto.RateResponse;
import daniel.nuud.currencyservice.exception.ResourceNotFoundException;
import daniel.nuud.currencyservice.notification.NotificationCommand;
import daniel.nuud.currencyservice.notification.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

    private final RatesProvider ratesProvider;
    private final NotificationProducer notificationProducer;

    public Mono<Double> convert(String from, String to, Double amount, String userKey) {
        final String base = normalize(from);
        final String quote = normalize(to);

        if (base.equals(quote)) {
            return notifySuccess(userKey, base, quote, amount, amount)
                    .thenReturn(amount);
        }

        return ratesProvider.getRates(base)
                .flatMap(map -> pickRate(map, quote))
                .map(rateStr -> multiply(amount, rateStr))
                .flatMap(result -> notifySuccess(userKey, base, quote, amount, result)
                        .thenReturn(result))
                .onErrorResume(ex -> notifyFailure(userKey, base, quote, amount, ex)
                        .then(Mono.error(ex)));
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

    private Mono<Void> notifySuccess(String userKey, String base, String quote, Double amount, Double result) {
        var cmd = new NotificationCommand(
                UUID.randomUUID().toString(),
                userKey,
                "FX conversion",
                "%s → %s : %s = %s".formatted(base, quote, amount, result),
                "SUCCESS",
                "FX:OK:" + epochMinute(),
                Instant.now(),
                "currency-service",
                null
        );
        return notificationProducer.send(cmd)
                .doOnError(e -> log.warn("notifySuccess publish failed: {}", e.toString()));
    }

    private Mono<Void> notifyFailure(String userKey, String base, String quote, Double amount, Throwable e) {
        var cmd = new NotificationCommand(
                UUID.randomUUID().toString(),
                userKey,
                "Conversion failed",
                "Failed to convert %s %s to %s: %s".formatted(amount, base, quote, e.getMessage()),
                "ERROR",
                "FX:INVALID:" + epochMinute(),
                Instant.now(),
                "currency-service",
                null
        );
        return notificationProducer.send(cmd)
                .doOnError(err -> log.warn("notifyFailure publish failed: {}", err.toString()));
    }

    private Long epochMinute() {
        return Instant.now().getEpochSecond() / 60;
    }
}
