package daniel.nuud.historicalservice.service;

import daniel.nuud.historicalservice.dto.ApiResponse;
import daniel.nuud.historicalservice.dto.StockBarApi;
import daniel.nuud.historicalservice.dto.StockPrice;
import daniel.nuud.historicalservice.model.StockBar;
import daniel.nuud.historicalservice.model.TimePreset;
import daniel.nuud.historicalservice.notification.NotificationCommand;
import daniel.nuud.historicalservice.notification.NotificationProducer;
import daniel.nuud.historicalservice.repository.StockBarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
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
    private final StockBarRepository stockBarRepository;
    private final NotificationProducer notificationProducer;

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
        String ticker = normalize(rawTicker);
        var p = determinePeriod(period);
        LocalDateTime fromDate = p.with(LocalTime.MIN);

        TimePreset preset = determinePreset(period);
        String multiplier = preset.multiplier();
        String timespan   = preset.timespan();

        return polygonClient.getAggregates(ticker, multiplier, timespan,
                        fromDate.toLocalDate(), toNow.toLocalDate(), apiKey)
                .switchIfEmpty(Mono.defer(() ->
                        notifyFailed(userKey, ticker, period, null)
                                .then(Mono.error(new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "No bars for " + ticker + " (" + period + ")")))))
                .flatMapMany(resp -> toBars(resp, ticker))
                .concatWith(appendLatestIfAny(ticker))
                .concatWith(Mono.defer(() -> notifyReady(userKey, ticker, period))
                        .then(Mono.empty()))
                .onErrorResume(ex ->
                        notifyFailed(userKey, ticker, period, ex instanceof Exception ? (Exception) ex : new RuntimeException(ex))
                                .then(Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Upstream error", ex))))
        ;

    }

    private Mono<Void> notifyReady(String userKey, String ticker, String period) {
        String t = ticker == null ? "" : ticker.toUpperCase(Locale.ROOT);
        String p = period == null ? "" : period.toUpperCase(Locale.ROOT);

        NotificationCommand cmd = new NotificationCommand(
                UUID.randomUUID().toString(),
                userKey,
                "Chart ready",
                t + " (" + p + ") is ready",
                "INFO",
                "CHART:READY:" + t + ":" + p,
                Instant.now(),
                "historical",
                null
        );

        return notificationProducer.send(cmd)
                .onErrorResume(e -> {
                    log.warn("Failed to send READY notification for {} ({}): {}", t, p, e.toString());
                    return Mono.empty();
                });
    }

    private Mono<Void> notifyFailed(String userKey, String ticker, String period, Throwable cause) {
        String t = ticker == null ? "" : ticker.toUpperCase(Locale.ROOT);
        String p = period == null ? "" : period.toUpperCase(Locale.ROOT);

        NotificationCommand cmd = new NotificationCommand(
                UUID.randomUUID().toString(),
                userKey,
                "Chart fetch failed",
                "Please try again later.",
                "ERROR",
                "CHART:FAILED:" + t + ":" + p,
                Instant.now(),
                "historical",
                null
        );

        return notificationProducer.send(cmd)
                .onErrorResume(e -> {
                    log.warn("Failed to send FAILED notification for {} ({}): {}", t, p, e.toString());
                    return Mono.empty();
                });
    }

    private Flux<StockBar> toBars(ApiResponse response, String ticker) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return Flux.empty();
        }

        return Flux.fromIterable(response.getResults())
                .map(dto -> mapToEntity(ticker, dto))
                .as(stockBarRepository::saveAll);
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
        long ts = p.getTimestamp(); // ms
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
        return switch (period.toUpperCase(Locale.ROOT)) {
            case "ONE_WEEK" -> new TimePreset("5", "minute");
            case "ONE_YEAR" -> new TimePreset("1", "week");
            default -> new TimePreset("1", "day");
        };
    }

    private LocalDateTime determinePeriod(String period) {
        LocalDateTime nowMax = toNow;
        return switch (daniel.nuud.historicalservice.model.Period.valueOf(period.toUpperCase(Locale.ROOT))) {
            case TODAY -> nowMax;
            case YESTERDAY -> nowMax.minusDays(1);
            case ONE_WEEK -> nowMax.minusWeeks(1);
            case ONE_MONTH -> nowMax.minusMonths(1);
            case ONE_YEAR -> nowMax.minusYears(1);
            case FIVE_YEARS -> nowMax.minusYears(5);
        };
    }

}
