package daniel.nuud.newsservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Article {

    @Id
    private String id;

    private String title;
    private String author;

    private String description;
    private String articleUrl;
    private String imageUrl;

    private String publishedUtc;
    private String publisherName;
    private String publisherLogoUrl;
    private String publisherHomepageUrl;
    private String publisherFaviconUrl;

    private List<String> tickers = new ArrayList<>();
}
