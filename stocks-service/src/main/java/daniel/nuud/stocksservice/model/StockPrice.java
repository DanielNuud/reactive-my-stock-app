package daniel.nuud.stocksservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StockPrice {

    private String symbol;
    private double price;
    private long timestamp;

}
