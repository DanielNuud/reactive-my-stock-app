package daniel.nuud.historicalservice.model;

import lombok.*;


import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockBar {

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
