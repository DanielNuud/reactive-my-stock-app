package daniel.nuud.companyinfoservice.service;

import daniel.nuud.companyinfoservice.dto.Ticker;
import daniel.nuud.companyinfoservice.exception.ResourceNotFoundException;
import daniel.nuud.companyinfoservice.model.TickerEntity;
import daniel.nuud.companyinfoservice.repository.TickerRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class TickerService {

    private final TickerRepository tickerRepository;
    private final PolygonClient polygonClient;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    @Bulkhead(name = "companyWrite", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "skipRefreshReactive")
    public Mono<Boolean> tryRefreshTickers(String rawQuery) {
        final String q = normalize(rawQuery);

        return polygonClient.searchTickers(q)
                .flatMapMany(resp -> Flux.fromIterable(resp.getResults() == null ? java.util.List.of() : resp.getResults()))
                .map(this::toEntityUpper)
                .buffer(100)
                .concatMap(batch -> Flux.fromIterable(batch).flatMap(this::upsertTicker, 8))
                .hasElements()
                .onErrorReturn(false);
    }

    @Cacheable(value = "tickerSuggest", key = "#rawQuery", sync = true)
    @Bulkhead(name = "companyRead", type = Bulkhead.Type.SEMAPHORE)
    public Mono<List<TickerEntity>> getFromDB(String rawQuery) {
        final String q = normalize(rawQuery);
        return tickerRepository.findTop5ByTickerStartsWithIgnoreCase(q)
                .collectList()
                .filter(list -> !list.isEmpty())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("TickerEntity with " + rawQuery + " not found")));
    }

    private Mono<TickerEntity> upsertTicker(TickerEntity t) {
        final String sql = """
            insert into tickers (ticker, company_name, currency)
            values ($1,$2,$3)
            on conflict (ticker) do update set
              company_name = excluded.company_name,
              currency     = excluded.currency
            returning ticker, company_name, currency
            """;
        return r2dbcEntityTemplate.getDatabaseClient()
                .sql(sql)
                .bind("$1", t.getTicker())
                .bind("$2", t.getCompanyName())
                .bind("$3", t.getCurrency())
                .map((row, md) -> r2dbcEntityTemplate.getConverter().read(TickerEntity.class, row, md))
                .one();
    }

    @SuppressWarnings("unused")
    private Mono<Boolean> skipRefreshReactive(String rawQuery, Throwable ex) {
        log.warn("Skip refresh for {}: {}", rawQuery, ex.toString());
        return Mono.just(false);
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private TickerEntity toEntityUpper(Ticker dto) {
        return new TickerEntity(
                dto.getTicker() == null ? "" : dto.getTicker().toUpperCase(Locale.ROOT),
                dto.getName(),
                dto.getCurrencyName()
        );
    }

}
