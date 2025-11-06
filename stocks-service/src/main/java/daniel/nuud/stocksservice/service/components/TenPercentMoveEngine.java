package daniel.nuud.stocksservice.service.components;

import daniel.nuud.stocksservice.model.StockPrice;
import daniel.nuud.stocksservice.notification.NotificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
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

    // последний «якорный» курс по тикеру,
    // относительно которого считаем 10% движение
    private final ConcurrentMap<String, Double> anchor = new ConcurrentHashMap<>();

    public Mono<Void> onPrice(StockPrice price) {
        final String ticker = price.getTicker();
        final double curr = price.getPrice();

        // если якоря нет — ставим и выходим (не уведомляем на первом значении)
        Double ref = anchor.putIfAbsent(ticker, curr);
        if (ref == null || ref == 0.0) return Mono.empty();

        double change = (curr - ref) / ref;
        if (Math.abs(change) < threshold) return Mono.empty();

        String dir = change >= 0 ? "UP" : "DOWN";
        double pct = BigDecimal.valueOf(Math.abs(change) * 100.0)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        // обновляем якорь до текущего
        anchor.put(ticker, curr);

        // все пользователи, для кого сейчас активен этот тикер
        Set<String> users = active.usersOf(ticker);
        if (users.isEmpty()) return Mono.empty();

        String title = "Price move " + dir;
        String message = ticker + " moved " + pct + "%  from " +
                round(ref, 2) + " to " + round(curr, 2);

        return Flux.fromIterable(users)
                .flatMap(userKey -> notificationClient.sendNotification(
                        userKey,
                        title,
                        message,
                        "WARN",
                        "STOCKS:MOVE10::" + ticker + ":" + dir
                ))
                .doOnComplete(() -> log.info("Move10 {} [{}%] -> notified: {}", ticker, pct, users))
                .doOnError(err -> log.warn("Move10 notify failed: {}", err.toString()))
                .then();
    }

    private static double round(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
