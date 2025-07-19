package daniel.nuud.companyinfoservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Branding {

    @JsonProperty("icon_url")
    private String iconUrl;

    @JsonProperty("logo_url")
    private String logoUrl;
}
