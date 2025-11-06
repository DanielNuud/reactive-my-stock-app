package daniel.nuud.historicalservice.service;

import daniel.nuud.historicalservice.dto.ApiResponse;
import daniel.nuud.historicalservice.dto.StockBarApi;
import daniel.nuud.historicalservice.dto.StockPrice;
import daniel.nuud.historicalservice.model.StockBar;
import daniel.nuud.historicalservice.model.TimePreset;
import daniel.nuud.historicalservice.model.Period;
import daniel.nuud.historicalservice.notification.NotificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class HistoricalService {

    private final PolygonClient polygonClient;
    private final NotificationClient notificationClient;

    @Value("${polygon.api.key}")
    private String apiKey;

    private static final ZoneId ZONE = ZoneOffset.UTC;

    private final Map<String, StockPrice> latestPrices = new ConcurrentHashMap<>();

    private final LocalDateTime toNow = LocalDateTime.now().with(LocalTime.MAX);

    public void saveRealtimePrice(StockPrice stockPrice) {
        log.info("Received real-time price: {}", stockPrice);
        latestPrices.put(stockPrice.getTicker(), stockPrice);
    }

    public Flux<StockBar> getHistoricalStockBar(String rawTicker, String period, String userKey) {
        final String ticker = normalize(rawTicker);

        // как в традиционном: from = determinePeriod(toNow).with(MIN), to = toNow
        final Period p = Period.valueOf(period.toUpperCase(Locale.ROOT));
        final LocalDateTime fromDate = determinePeriod(p).with(LocalTime.MIN);

        final TimePreset preset = determinePreset(period);

        return polygonClient.getAggregates(
                        ticker,
                        preset.multiplier(),
                        preset.timespan(),
                        fromDate.toLocalDate(),
                        toNow.toLocalDate(),
                        apiKey
                )
                .flatMapMany(resp -> {
                    if (resp != null && resp.getResults() != null) {
                        // 1) превращаем результаты в Flux баров
                        Flux<StockBar> bars = Flux.fromIterable(resp.getResults())
                                .map(dto -> mapToEntity(ticker, dto));
                        // 2) добавляем последний «реалтайм»-бар (как в традиционном)
                        bars = bars.concatWith(appendLatestIfAny(ticker));
                        // 3) шлём уведомление об успехе (fire-and-forget)
                        return bars.concatWith(
                                Mono.defer(() -> notifyReady(userKey, ticker, period)
                                        .onErrorResume(e -> { log.warn("notifyReady error: {}", e.toString()); return Mono.empty(); })
                                ).then(Mono.empty())
                        );
                    }
                    return Mono.defer(() -> notifyFailed(userKey, ticker, period, null)
                            .onErrorResume(e -> { log.warn("notifyFailed error: {}", e.toString()); return Mono.empty(); })
                    ).thenMany(Flux.empty());
                })
                .onErrorResume(e -> {
                    log.error("Error while fetching stock bar", e);
                    return notifyFailed(userKey, ticker, period, e)
                            .onErrorResume(err -> Mono.empty()) // не даём уведомление «уронить» поток
                            .thenMany(Flux.empty());
                });
    }

    private Mono<Void> notifyReady(String userKey, String ticker, String period) {
        return notificationClient.sendNotification(
                userKey,
                "Chart ready",
                ticker.toUpperCase() + " (" + period + ") is ready",
                "INFO",
                "CHART:READY:" + ticker.toUpperCase() + ":" + period
        );
    }

    private Mono<Void> notifyFailed(String userKey, String ticker, String period, Throwable cause) {
        return notificationClient.sendNotification(
                userKey,
                "Chart fetch failed",
                "Please try again later.",
                "ERROR",
                "CHART:FAILED:" + ticker.toUpperCase() + ":" + period.toUpperCase()
        );
    }


    private Flux<StockBar> toBars(ApiResponse response, String ticker) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return Flux.empty();
        }

        return Flux.fromIterable(response.getResults())
                .map(dto -> mapToEntity(ticker, dto));
    }

    private Flux<StockBar> appendLatestIfAny(String ticker) {
        StockPrice latest = latestPrices.get(ticker);
        if (latest == null) return Flux.empty();
        return Mono.just(convertToStockBar(latest)).flux();
    }

    private StockBar mapToEntity(String ticker, StockBarApi dto) {
        long ts = dto.getTimestamp();
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZONE);

        return StockBar.builder()
                .id(normalize(ticker) + ":" + ts)
                .ticker(normalize(ticker))
                .timestamp(ts)
                .date(dt)
                .openPrice(dto.getOpenPrice())
                .highPrice(dto.getHighPrice())
                .lowPrice(dto.getLowPrice())
                .closePrice(dto.getClosePrice())
                .volume(dto.getVolume())
                .numberOfTransactions(dto.getNumberOfTransactions())
                .build();
    }

    private StockBar convertToStockBar(StockPrice p) {
        long ts = p.getTimestamp();
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZONE);

        return StockBar.builder()
                .id(normalize(p.getTicker()) + ":" + ts)
                .ticker(normalize(p.getTicker()))
                .timestamp(ts)
                .date(dt)
                .openPrice(p.getPrice())
                .highPrice(p.getPrice())
                .lowPrice(p.getPrice())
                .closePrice(p.getPrice())
                .volume(0)
                .numberOfTransactions(0)
                .build();
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private TimePreset determinePreset(String period) {
        return switch (period.toUpperCase()) {
            case "ONE_WEEK" -> new TimePreset("5", "minute");
            case "ONE_MONTH" -> new TimePreset("1", "day");
            case "ONE_YEAR" -> new TimePreset("1", "week");
            default -> new TimePreset("1", "day"); // fallback
        };
    }

    private LocalDateTime determinePeriod(Period from) {
        return switch (from) {
            case TODAY       -> toNow;
            case YESTERDAY   -> toNow.minusDays(1);
            case ONE_WEEK    -> toNow.minusWeeks(1);
            case ONE_MONTH   -> toNow.minusMonths(1);
            case ONE_YEAR    -> toNow.minusYears(1);
            case FIVE_YEARS  -> toNow.minusYears(5);
        };
    }

}
