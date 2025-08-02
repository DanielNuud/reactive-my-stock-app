package daniel.nuud.historicalservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "stock_bars")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockBar {

    @Id
    private String id;

    private String ticker;
    private String date;

    private Double closePrice;

    private Double lowPrice;

    private Double highPrice;

    private Double openPrice;

    private Integer volume;

    private Integer numberOfTransactions;

    private Long timestamp;
}
