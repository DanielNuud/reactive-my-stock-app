package daniel.nuud.newsservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Publisher {

    @JsonProperty("favicon_url")
    private String favicon;

    private String name;

    @JsonProperty("homepage_url")
    private String homepageUrl;

    @JsonProperty("logo_url")
    private String logoUrl;
}
