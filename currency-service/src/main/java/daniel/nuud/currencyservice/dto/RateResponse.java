package daniel.nuud.currencyservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class RateResponse {
    @JsonProperty("data")
    private Map<String, String> rates;
}