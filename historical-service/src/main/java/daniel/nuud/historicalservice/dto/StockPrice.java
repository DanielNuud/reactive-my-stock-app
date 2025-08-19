package daniel.nuud.historicalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockPrice {
    private String ticker;
    private double price;
    private long timestamp;
}
