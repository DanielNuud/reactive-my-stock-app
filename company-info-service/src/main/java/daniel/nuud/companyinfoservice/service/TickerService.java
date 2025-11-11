package daniel.nuud.companyinfoservice.service;

import daniel.nuud.companyinfoservice.dto.Ticker;
import daniel.nuud.companyinfoservice.exception.ResourceNotFoundException;
import daniel.nuud.companyinfoservice.model.TickerEntity;
import daniel.nuud.companyinfoservice.repository.TickerRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class TickerService {

    private final TickerRepository tickerRepository;
    private final PolygonClient polygonClient;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private static final int BATCH_SIZE = 150;

    public Mono<Boolean> tryRefreshTickers(String rawQuery) {
        final String q = normalize(rawQuery);

        return polygonClient.searchTickers(q)
                .map(resp -> resp.getResults() == null ? List.<Ticker>of() : resp.getResults())
                .flatMapMany(list -> {
                    if (list.isEmpty()) return Flux.empty();
                    return Flux.fromIterable(list).map(this::toEntityUpper);
                })
                .buffer(BATCH_SIZE)
                .concatMap(this::upsertBatch, 4)
                .reduce(0, Integer::sum)
                .map(cnt -> cnt > 0)
                .thenReturn(true)
                .onErrorReturn(false);
    }

    private Mono<Integer> upsertBatch(List<TickerEntity> batch) {
        StringBuilder sb = new StringBuilder("INSERT INTO tickers (ticker, company_name, currency) VALUES ");
        for (int i = 0; i < batch.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append("($").append(3*i+1).append(",$").append(3*i+2).append(",$").append(3*i+3).append(')');
        }
        sb.append("""
        ON CONFLICT (ticker) DO UPDATE
        SET company_name = EXCLUDED.company_name,
            currency     = EXCLUDED.currency
        WHERE tickers.company_name IS DISTINCT FROM EXCLUDED.company_name
           OR tickers.currency     IS DISTINCT FROM EXCLUDED.currency
        """);
        String sql = sb.toString();

        return r2dbcEntityTemplate.getDatabaseClient().inConnectionMany(conn -> {
            Statement st = conn.createStatement(sql);
            int idx = 0;
            for (TickerEntity t : batch) {
                st.bind(idx++, t.getTicker());
                st.bind(idx++, t.getCompanyName());
                st.bind(idx++, t.getCurrency());
            }
            return Flux.from(st.execute()).flatMap(Result::getRowsUpdated);
        })
                .reduce(0L, Long::sum)
                .map(Long::intValue);
    }

    @SuppressWarnings("unused")
    public Mono<Boolean> skipRefreshReactive(String rawQuery, Throwable ex) {
        log.warn("Skip refresh for {}: {}", rawQuery, ex.toString());
        return Mono.just(false);
    }

//    @Cacheable(value = "tickerSuggest", key = "#rawQuery")
    public Mono<List<TickerEntity>> getFromDB(String rawQuery) {
        final String q = normalize(rawQuery);
        return tickerRepository.findTop5ByTickerStartsWith(q)
                .collectList();
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
