package daniel.nuud.stocksservice.service.components;

import daniel.nuud.stocksservice.dto.StockPriceDto;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PricesHub {

    // ticker -> стрим по тикеру
    private final ConcurrentHashMap<String, Sinks.Many<StockPriceDto>> sinks = new ConcurrentHashMap<>();
    // ticker -> число активных WS-подписок (для логов/отладки или first/last, если понадобится)
    private final ConcurrentHashMap<String, AtomicInteger> refCnt = new ConcurrentHashMap<>();

    public Flux<StockPriceDto> fluxFor(String rawTicker) {
        final String ticker = norm(rawTicker);
        var sink = sinks.computeIfAbsent(ticker, t -> Sinks.many().multicast().onBackpressureBuffer());
        var cnt  = refCnt.computeIfAbsent(ticker, t -> new AtomicInteger(0));

        return sink.asFlux()
                .doOnSubscribe(s -> cnt.incrementAndGet())
                .doFinally(sig -> {
                    if (cnt.decrementAndGet() <= 0) {
                        sinks.remove(ticker, sink);
                        refCnt.remove(ticker);
                    }
                });
    }

    public void emit(String rawTicker, StockPriceDto dto) {
        final String ticker = norm(rawTicker);
        var sink = sinks.get(ticker);
        if (sink != null) sink.tryEmitNext(dto);
    }


    private String norm(String t) {
        return t == null ? "" : t.trim().toUpperCase();
    }
}
