package daniel.nuud.stocksservice.service.components;

import daniel.nuud.stocksservice.model.StockPrice;
import daniel.nuud.stocksservice.notification.NotificationCommand;
import daniel.nuud.stocksservice.notification.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    private final NotificationPublisher notifications;

    private final ConcurrentMap<String, Double> anchor = new ConcurrentHashMap<>();

    public void onPrice(StockPrice price) {
        final String ticker = price.getTicker();
        final double curr = price.getPrice();
        Double ref = anchor.putIfAbsent(ticker, curr);
        if (ref == null || ref == 0.0) return;

        double change = (curr - ref) / ref;
        if (Math.abs(change) >= threshold) {
            String dir = change >= 0 ? "UP" : "DOWN";
            double pct = round(Math.abs(change) * 100.0, 2);

            active.owner().ifPresentOrElse(userKey -> {
                notifications.publish(new NotificationCommand(
                        userKey,
                        "Price move " + dir,
                        ticker + " moved " + pct + "% from ~" + round(ref, 4),
                        "WARN",
                        "STOCKS:MOVE10:" + ticker + ":" + dir,
                        System.currentTimeMillis()
                ));
                log.info("Move10 {} {}% → notified user={}", ticker, pct, userKey);
            }, () -> log.debug("Move10 {} {}% but no active subscriber", ticker, pct));

            anchor.put(ticker, curr);
        }
    }

    private static double round(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
