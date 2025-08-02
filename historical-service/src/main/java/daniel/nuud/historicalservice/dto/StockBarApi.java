package daniel.nuud.historicalservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StockBarApi {

    @JsonProperty("c")
    private Double closePrice;

    @JsonProperty("h")
    private Double highPrice;

    @JsonProperty("l")
    private Double lowPrice;

    @JsonProperty("n")
    private Integer numberOfTransactions;

    @JsonProperty("o")
    private Double openPrice;

    @JsonProperty("t")
    private Long timestamp;

    @JsonProperty("v")
    private Integer volume;

    @JsonProperty("vw")
    private Double volumeWeight;
}
