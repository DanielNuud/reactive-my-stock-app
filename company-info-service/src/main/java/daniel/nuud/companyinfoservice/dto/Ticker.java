package daniel.nuud.companyinfoservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Ticker {

    private String ticker;

    private String name;

    @JsonProperty("currency_name")
    private String currencyName;

}
