package daniel.nuud.historicalservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "stock_bars")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndex(name = "uq_ticker_ts", def = "{'ticker':1,'timestamp':1}", unique = true)
public class StockBar {

    @Id
    private String id;

    private String ticker;
    private LocalDateTime date;

    private Double closePrice;

    private Double lowPrice;

    private Double highPrice;

    private Double openPrice;

    private Integer volume;

    private Integer numberOfTransactions;

    private Long timestamp;
}
