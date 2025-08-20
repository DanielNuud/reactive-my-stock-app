package daniel.nuud.stocksservice.model;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockPrice {

    private String ticker;
    private double price;
    private long timestamp;

}
