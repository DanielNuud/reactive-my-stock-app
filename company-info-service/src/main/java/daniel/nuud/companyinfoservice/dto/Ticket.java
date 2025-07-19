package daniel.nuud.companyinfoservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Ticket {

    private String ticker;

    private Branding branding;

    private Address address;

    private String description;

    @JsonProperty("homepage_url")
    private String homepageUrl;

    private String name;

    @JsonProperty("primary_exchange")
    private String primaryExchange;

    @JsonProperty("market_cap")
    private Long marketCap;
}
