package daniel.nuud.newsservice.repository;

import daniel.nuud.newsservice.model.Article;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.Optional;

@Repository
public interface NewsRepository extends R2dbcRepository<Article, String> {
    @Query("""
           select id, title, author, description, article_url, image_url,
                  published_utc, publisher_name, publisher_logo_url,
                  publisher_homepage_url, publisher_favicon_url, tickers
             from articles
            where $1 = any(tickers)
            order by published_utc desc
            limit 5
           """)
    Flux<Article> findTop5ByTickersOrderByPublishedUtcDesc(String ticker);
}
