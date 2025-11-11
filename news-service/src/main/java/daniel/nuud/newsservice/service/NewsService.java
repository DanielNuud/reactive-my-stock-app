package daniel.nuud.newsservice.service;

import daniel.nuud.newsservice.dto.ApiArticle;
import daniel.nuud.newsservice.exception.ResourceNotFoundException;
import daniel.nuud.newsservice.model.Article;
import daniel.nuud.newsservice.repository.NewsRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private final PolygonClient polygonClient;
    private final NewsRepository newsRepository;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    public Mono<Boolean> skipRefreshReactive(String ticker, Throwable ex) {
        log.warn("Skip news refresh for {}: {}", ticker, ex.toString());
        return Mono.just(false);
    }

//    @Bulkhead(name = "newsWrite", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "skipRefreshReactive")
public Mono<Boolean> tryRefreshNews(String rawTicker) {
    final String ticker = normalize(rawTicker);

    return polygonClient.getApiResponse(ticker)
            .map(resp -> resp.getResults() == null ? List.<ApiArticle>of() : resp.getResults())
            .flatMap(list -> {
                if (list.isEmpty()) return Mono.just(false);

                return Flux.fromIterable(list)
                        .map(api -> toEntity(api, ticker))
                        .buffer(100)
                        .concatMap(this::upsertBatch)
                        .reduce(0, Integer::sum)
                        .map(total -> total > 0);
            });
}

private Mono<Integer> upsertBatch(List<Article> batch) {
    if (batch.isEmpty()) return Mono.just(0);

    StringBuilder sb = new StringBuilder("""
            INSERT INTO articles
              (title, author, description, article_url, image_url,
               published_utc, publisher_name, publisher_logo_url,
               publisher_homepage_url, publisher_favicon_url, tickers)
            VALUES
            """);

    for (int i = 0; i < batch.size(); i++) {
        if (i > 0) sb.append(',');
        int p = i * 11;
        sb.append("(")
                .append("$").append(p + 1).append(",")
                .append("$").append(p + 2).append(",")
                .append("$").append(p + 3).append(",")
                .append("$").append(p + 4).append(",")
                .append("$").append(p + 5).append(",")
                .append("$").append(p + 6).append(",")
                .append("$").append(p + 7).append(",")
                .append("$").append(p + 8).append(",")
                .append("$").append(p + 9).append(",")
                .append("$").append(p +10).append(",")
                .append("$").append(p +11).append(")");
    }

    sb.append("""
  ON CONFLICT (article_url) DO UPDATE SET
    title                  = EXCLUDED.title,
    author                 = EXCLUDED.author,
    description            = EXCLUDED.description,
    image_url              = EXCLUDED.image_url,
    published_utc          = EXCLUDED.published_utc,
    publisher_name         = EXCLUDED.publisher_name,
    publisher_logo_url     = EXCLUDED.publisher_logo_url,
    publisher_homepage_url = EXCLUDED.publisher_homepage_url,
    publisher_favicon_url  = EXCLUDED.publisher_favicon_url,
    tickers                = EXCLUDED.tickers
  WHERE
    (articles.title,
     articles.author,
     articles.description,
     articles.image_url,
     articles.published_utc,
     articles.publisher_name,
     articles.publisher_logo_url,
     articles.publisher_homepage_url,
     articles.publisher_favicon_url,
     articles.tickers)
  IS DISTINCT FROM
    (EXCLUDED.title,
     EXCLUDED.author,
     EXCLUDED.description,
     EXCLUDED.image_url,
     EXCLUDED.published_utc,
     EXCLUDED.publisher_name,
     EXCLUDED.publisher_logo_url,
     EXCLUDED.publisher_homepage_url,
     EXCLUDED.publisher_favicon_url,
     EXCLUDED.tickers)
""");

    String sql = sb.toString();

    return r2dbcEntityTemplate.getDatabaseClient()
            .inConnectionMany(conn -> {
                Statement st = conn.createStatement(sql);
                int idx = 0;
                for (Article a : batch) {
                    bindNullable(st, idx++, a.getTitle(),                String.class);
                    bindNullable(st, idx++, a.getAuthor(),               String.class);
                    bindNullable(st, idx++, a.getDescription(),          String.class);
                    bindNullable(st, idx++, a.getArticleUrl(),           String.class);
                    bindNullable(st, idx++, a.getImageUrl(),             String.class);
                    bindNullable(st, idx++, a.getPublishedUtc(), java.time.Instant.class);
                    bindNullable(st, idx++, a.getPublisherName(),        String.class);
                    bindNullable(st, idx++, a.getPublisherLogoUrl(),     String.class);
                    bindNullable(st, idx++, a.getPublisherHomepageUrl(), String.class);
                    bindNullable(st, idx++, a.getPublisherFaviconUrl(),  String.class);
                    bindNullable(st, idx++, a.getTickers(),              String[].class);
                }
                return Flux.from(st.execute()).flatMap(Result::getRowsUpdated);
            })
            .reduce(0L, Long::sum)
            .map(Long::intValue);
}

    private static <T> void bindNullable(Statement st, int index, @Nullable T value, Class<T> type) {
        if (value == null) {
            st.bindNull(index, type);
        } else {
            st.bind(index, value);
        }
    }

    public Mono<List<Article>> getTop5NewsByTicker(String rawTicker) {
        final String ticker = normalize(rawTicker);
        return newsRepository.findTop5ByTickersOrderByPublishedUtcDesc(ticker)
                .collectList();
    }

    private Mono<Boolean> skipTop5News(String ticker, Throwable ex) {
        log.warn("Skip news top5 for {}: {}", ticker, ex.toString());
        return Mono.just(false);
    }

    private static String nvl(String v, String d) { return v != null ? v : d; }

    private Article toEntity(ApiArticle api, String ticker) {

        return Article.builder()
                .title(nvl(api.getTitle(), ""))
                .author(api.getAuthor())
                .description(api.getDescription())
                .articleUrl(api.getArticleUrl())
                .imageUrl(api.getImageUrl())
                .publishedUtc(parseInstant(api.getPublishedUtc()))
                .publisherName(api.getPublisher().getName())
                .publisherLogoUrl(api.getPublisher().getLogoUrl())
                .publisherHomepageUrl(api.getPublisher().getHomepageUrl())
                .publisherFaviconUrl(api.getPublisher().getFavicon())
                .tickers(new String[]{ ticker })
                .build();
    }

    private static Instant parseInstant(String s) {
        if (s == null || s.isBlank()) return Instant.EPOCH;
        try { return Instant.parse(s); }
        catch (Exception e) { return OffsetDateTime.parse(s).toInstant(); }
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

}
