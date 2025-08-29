package daniel.nuud.newsservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Table("articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    private String id;

    private String title;
    private String author;
    private String description;

    @Column("article_url")
    private String articleUrl;

    @Column("image_url")
    private String imageUrl;

    @Column("published_utc")
    private Instant publishedUtc;

    @Column("publisher_name")
    private String publisherName;

    @Column("publisher_logo_url")
    private String publisherLogoUrl;

    @Column("publisher_homepage_url")
    private String publisherHomepageUrl;

    @Column("publisher_favicon_url")
    private String publisherFaviconUrl;

    private String[] tickers;
}
