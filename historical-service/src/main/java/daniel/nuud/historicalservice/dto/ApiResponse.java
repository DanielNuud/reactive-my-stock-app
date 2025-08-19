package daniel.nuud.historicalservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ApiResponse {
    private String ticker;
    private Integer queryCount;
    private Integer resultsCount;
    private Boolean adjusted;
    @JsonProperty("results")
    private List<StockBarApi> results;
}
