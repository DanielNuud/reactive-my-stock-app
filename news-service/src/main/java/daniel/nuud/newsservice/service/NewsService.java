package daniel.nuud.newsservice.service;

import daniel.nuud.newsservice.dto.ApiArticle;
import daniel.nuud.newsservice.exception.ResourceNotFoundException;
import daniel.nuud.newsservice.model.Article;
import daniel.nuud.newsservice.repository.NewsRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
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

    private Mono<Boolean> skipRefreshReactive(String ticker, Throwable ex) {
        log.warn("Skip news refresh for {}: {}", ticker, ex.toString());
        return Mono.just(false);
    }

    @Bulkhead(name = "newsWrite", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "skipRefreshReactive")
    public Mono<Boolean> tryRefreshNews(String rawTicker) {
        final String ticker = normalize(rawTicker);

        return polygonClient.getApiResponse(ticker)
                .map(resp -> resp.getResults() == null ? List.<ApiArticle>of() : resp.getResults())
                .flatMap(list -> {
                    if (list.isEmpty()) return Mono.just(false);

                    return Flux.fromIterable(list)
                            .map(api -> toEntity(api, ticker))
                            .concatMap(this::upsertArticle)
                            .then(Mono.just(true));
                })
                .onErrorReturn(false);
    }

    private Mono<Article> upsertArticle(Article a) {
        final String sql = """
            insert into articles
              (title, author, description, article_url, image_url,
               published_utc, publisher_name, publisher_logo_url,
               publisher_homepage_url, publisher_favicon_url, tickers)
            values ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)
            on conflict (article_url) do update set
              title                  = excluded.title,
              author                 = excluded.author,
              description            = excluded.description,
              image_url              = excluded.image_url,
              published_utc          = excluded.published_utc,
              publisher_name         = excluded.publisher_name,
              publisher_logo_url     = excluded.publisher_logo_url,
              publisher_homepage_url = excluded.publisher_homepage_url,
              publisher_favicon_url  = excluded.publisher_favicon_url,
              tickers                = excluded.tickers
            returning id, title, author, description, article_url, image_url,
                      published_utc, publisher_name, publisher_logo_url,
                      publisher_homepage_url, publisher_favicon_url, tickers
            """;
        return r2dbcEntityTemplate.getDatabaseClient()
                .sql(sql)
                .bind("$1",  a.getTitle())
                .bind("$2",  a.getAuthor())
                .bind("$3",  a.getDescription())
                .bind("$4",  a.getArticleUrl())
                .bind("$5",  a.getImageUrl())
                .bind("$6",  a.getPublishedUtc())
                .bind("$7",  a.getPublisherName())
                .bind("$8",  a.getPublisherLogoUrl())
                .bind("$9",  a.getPublisherHomepageUrl())
                .bind("$10", a.getPublisherFaviconUrl())
                .bind("$11", a.getTickers())
                .map((row, md) -> r2dbcEntityTemplate.getConverter().read(Article.class, row, md))
                .one();
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
