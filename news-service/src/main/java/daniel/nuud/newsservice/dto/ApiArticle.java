package daniel.nuud.newsservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApiArticle {

    @JsonProperty("article_url")
    private String articleUrl;

    private String title;
    private String description;
    private String id;

    @JsonProperty("image_url")
    private String imageUrl;

    private String author;

    private List<String> tickers;

    @JsonProperty("published_utc")
    private String publishedUtc;

    private Publisher publisher;
}
