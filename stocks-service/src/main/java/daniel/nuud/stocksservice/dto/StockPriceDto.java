package daniel.nuud.stocksservice.dto;

import daniel.nuud.stocksservice.model.StockPrice;
import lombok.Builder;

@Builder
public record StockPriceDto(
        String ticker,
        Double price,
        Long timestamp
) {
    public static StockPriceDto from(StockPrice p) {
        return StockPriceDto.builder()
                .ticker(p.getTicker())
                .price(p.getPrice())
                .timestamp(p.getTimestamp())
                .build();
    }
}