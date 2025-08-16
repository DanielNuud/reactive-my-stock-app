package daniel.nuud.companyinfoservice.service;

import com.mongodb.DuplicateKeyException;
import daniel.nuud.companyinfoservice.dto.Ticker;
import daniel.nuud.companyinfoservice.exception.ResourceNotFoundException;
import daniel.nuud.companyinfoservice.model.TickerEntity;
import daniel.nuud.companyinfoservice.repository.TickerRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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

    @Bulkhead(name = "companyWrite", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "skipRefreshReactive")
    public Mono<Boolean> tryRefreshTickers(String rawQuery) {
        final String q = normalize(rawQuery);

        return polygonClient.searchTickers(q)                                   // Mono<TickerApiResponse>
                .flatMapMany(resp -> Flux.fromIterable(
                        resp.getResults() == null ? List.of() : resp.getResults())) // Flux<TickerEntity>
                .map(this::toEntityUpper)                                           // Flux<TickerEntity>
                .collectList()                                                      // Mono<List<TickerEntity>>
                .flatMap(list -> list.isEmpty()
                        ? Mono.just(false)
                        : tickerRepository.insert(list)                                  // ВАЖНО: insert, не save
                        .onErrorContinue(DuplicateKeyException.class, (ex, obj) -> {}) // игнор дубликатов
                        .hasElements()                                               // true, если хоть один реально вставился
                )
                .onErrorReturn(false);
    }


    private String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private TickerEntity toEntityUpper(Ticker dto) {
        return new TickerEntity(
                dto.getTicker() == null ? "" : dto.getTicker().toUpperCase(Locale.ROOT),
                dto.getCurrencyName(),
                dto.getName()
        );
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
}
