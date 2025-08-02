package daniel.nuud.historicalservice.service;

import daniel.nuud.historicalservice.dto.ApiResponse;
import daniel.nuud.historicalservice.dto.StockBarApi;
import daniel.nuud.historicalservice.exception.ResourceNotFoundException;
import daniel.nuud.historicalservice.model.Period;
import daniel.nuud.historicalservice.model.StockBar;
import daniel.nuud.historicalservice.model.Timespan;
import daniel.nuud.historicalservice.repository.StockBarRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class HistoricalService {

    private final WebClient webClient;

    private final StockBarRepository stockBarRepository;

    @Value("${polygon.api.key}")
    private String apiKey;

    public HistoricalService(WebClient webClient, StockBarRepository stockBarRepository) {
        this.webClient = webClient;
        this.stockBarRepository = stockBarRepository;
    }

    private final LocalDate toNow = LocalDate.now();

    private LocalDate determinePeriod(Period from) {

        return switch (from) {
            case TODAY -> toNow;
            case YESTERDAY -> toNow.minusDays(1);
            case ONE_WEEK -> toNow.minusWeeks(1);
            case ONE_MONTH -> toNow.minusMonths(1);
            case ONE_YEAR -> toNow.minusYears(1);
            case FIVE_YEARS -> toNow.minusYears(5);
        };
    }

    private String determineTimespan(Timespan timespan) {
        return switch (timespan) {
            case SECOND -> "SECOND";
            case MINUTE -> "MINUTE";
            case HOUR -> "HOUR";
            case DAY -> "DAY";
            case WEEK -> "WEEK";
            case MONTH -> "MONTH";
            case QUARTER ->  "QUARTER";
            case YEAR -> "YEAR";
        };
    }

    private Mono<ApiResponse> fetchHistoricalData(String ticker, Integer multiplier, String timespan, LocalDate from, LocalDate to, String apiKey) {
        log.info("Fetching stock bar for ticker {}", ticker);
        return webClient.get()
                .uri("/v2/aggs/ticker/{ticker}/range/{multiplier}/{timespan}/{from}/{to}?adjusted=true&sort=asc&apiKey={apiKey}",
                        ticker, multiplier, timespan, from, to, apiKey)
                .retrieve()
                .bodyToMono(ApiResponse.class)
                .filter(resp -> resp != null && resp.getResults() != null)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Data not found")));
    }

    public Flux<StockBar> getHistoricalStockBar(String ticker, String period, Integer multiplier, String timespan) {
        Period fromStringToPeriod = Period.valueOf(period.toUpperCase());
        LocalDate fromDate = determinePeriod(fromStringToPeriod);
        String toNowStr = toNow.toString();
        String fromDateStr = fromDate.toString();

        return stockBarRepository.findByTickerAndDateBetween(ticker, fromDateStr, toNowStr)
                .sort(Comparator.comparing(sb -> LocalDate.parse(sb.getDate())))
                .switchIfEmpty(fetchHistoricalData(ticker, multiplier, determineTimespan(Timespan.valueOf(timespan)).toLowerCase(), fromDate, toNow, apiKey)
                        .flatMapMany(resp -> Flux.fromIterable(resp.getResults())
                                .map(dto -> mapToEntity(ticker, dto))
                                .collectList()
                                .flatMapMany(stockBarRepository::saveAll)
                                .thenMany(stockBarRepository.findByTickerAndDateBetween(ticker, fromDateStr, toNowStr))
                                .sort(Comparator.comparing(sb -> LocalDate.parse(sb.getDate())))
                        )
                );
    }

    private StockBar mapToEntity(String ticker, StockBarApi dto) {
        Instant dateInstant = Instant.ofEpochMilli(dto.getTimestamp());
        LocalDate date = dateInstant.atZone(ZoneId.systemDefault()).toLocalDate();

        String id = ticker + "_" + date;

        return new StockBar(
                id,
                ticker,
                date.toString(),
                dto.getClosePrice(),
                dto.getLowPrice(),
                dto.getHighPrice(),
                dto.getOpenPrice(),
                dto.getVolume(),
                dto.getNumberOfTransactions(),
                dto.getTimestamp()
        );
    }

}
