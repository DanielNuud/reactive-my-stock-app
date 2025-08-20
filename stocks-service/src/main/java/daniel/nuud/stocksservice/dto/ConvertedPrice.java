package daniel.nuud.stocksservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConvertedPrice {
    private Double price;
    private String ticker;
    private Long timestamp;
    private String currentCurrency;
}
