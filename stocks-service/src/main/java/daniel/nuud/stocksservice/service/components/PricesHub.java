package daniel.nuud.stocksservice.service.components;

import daniel.nuud.stocksservice.dto.StockPriceDto;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class PricesHub {
    private final Sinks.Many<StockPriceDto> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public void emit(StockPriceDto dto) {
        sink.tryEmitNext(dto);
    }

    public Flux<StockPriceDto> stream() {
        return sink.asFlux();
    }
}
