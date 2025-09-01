package daniel.nuud.stocksservice.service.components;

import daniel.nuud.stocksservice.model.StockPrice;
import daniel.nuud.stocksservice.notification.NotificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenPercentMoveEngine {

    @Value("${alerts.move10.threshold:0.10}")
    private double threshold;

    private final ActiveSubscription active;
    private final NotificationClient notificationClient;

    private final ConcurrentMap<String, Double> anchor = new ConcurrentHashMap<>();

    public Mono<Void> onPrice(StockPrice price) {
        final String ticker = price.getTicker();
        final double curr = price.getPrice();

        Double ref = anchor.putIfAbsent(ticker, curr);
        if (ref == null || ref == 0.0) return Mono.empty();

        double change = (curr - ref) / ref;
        if (Math.abs(change) < threshold) return Mono.empty();

        String dir = change >= 0 ? "UP" : "DOWN";
        double pct = BigDecimal.valueOf(Math.abs(change) * 100.0)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        anchor.put(ticker, curr);

        return Mono.justOrEmpty(active.owner())
                .flatMap(userKey -> notificationClient.sendNotification(
                        userKey,
                        "Price move " + dir,
                        ticker + " moved " + pct + "% from",
                        "WARN",
                        "STOCKS:MOVE10:" + ticker + ":" + dir
                ))
                .doOnSuccess(v -> log.info("Move10 {} {}% → notified", ticker, pct))
                .doOnError(err -> log.warn("Move10 notify failed: {}", err.toString()))
                .then();
    }

    private static double round(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
