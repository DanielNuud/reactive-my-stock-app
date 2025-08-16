package daniel.nuud.newsservice.service;

import com.mongodb.DuplicateKeyException;
import daniel.nuud.newsservice.dto.ApiArticle;
import daniel.nuud.newsservice.exception.ResourceNotFoundException;
import daniel.nuud.newsservice.model.Article;
import daniel.nuud.newsservice.repository.NewsRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private final PolygonClient polygonClient;
    private final NewsRepository newsRepository;

    @SuppressWarnings("unused")
    private Mono<Boolean> skipRefreshReactive(String ticker, Throwable ex) {
        log.warn("Skip news refresh for {}: {}", ticker, ex.toString());
        return Mono.just(false);
    }

    @Bulkhead(name = "newsWrite", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "skipRefreshReactive")
    public Mono<Boolean> tryRefreshNews(String rawTicker) {
        final String ticker = normalize(rawTicker);

        return polygonClient.getApiResponse(ticker)
                .flatMapMany(resp -> Flux.fromIterable(
                        resp.getResults() == null ? List.<ApiArticle>of() : resp.getResults()))
                .map(api -> toEntity(api, ticker))
                .collectList()
                .flatMap(list -> list.isEmpty()
                        ? Mono.just(false)
                        : newsRepository.insert(list)
                        .onErrorContinue(DuplicateKeyException.class, (ex, obj) -> {})
                        .hasElements())
                .onErrorReturn(false);
    }

    @Bulkhead(name = "newsRead", type = Bulkhead.Type.SEMAPHORE)
    public Mono<List<Article>> getTop5NewsByTicker(String rawTicker) {
        final String ticker = normalize(rawTicker);
        return newsRepository.findTop5ByTickersOrderByPublishedUtcDesc(ticker)
                .collectList()
                .filter(list -> !list.isEmpty())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("News for " + ticker + " not found")));
    }

    private static String nvl(String v, String d) { return v != null ? v : d; }

    private Article toEntity(ApiArticle api, String fallbackTicker) {
        Article a = new Article();

        a.setTitle(nvl(api.getTitle(), "Not found"));
        a.setAuthor(nvl(api.getAuthor(), "Not found"));
        a.setDescription(nvl(api.getDescription(), "Not found"));
        a.setArticleUrl(nvl(api.getArticleUrl(), "Not found"));
        a.setImageUrl(nvl(api.getImageUrl(), "Not found"));

        a.setPublishedUtc(nvl(api.getPublishedUtc(), "Not found"));

        if (api.getPublisher() != null) {
            a.setPublisherName(nvl(api.getPublisher().getName(), "Not found"));
            a.setPublisherHomepageUrl(nvl(api.getPublisher().getHomepageUrl(), "Not found"));
            a.setPublisherLogoUrl(nvl(api.getPublisher().getLogoUrl(), "Not found"));
            a.setPublisherFaviconUrl(nvl(api.getPublisher().getFavicon(), "Not found"));
        }

        if (api.getTickers() != null && !api.getTickers().isEmpty()) {
            a.setTickers(api.getTickers());
        } else {
            a.setTickers(List.of(fallbackTicker));
        }
        return a;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    public Flux<Article> getTop5News(String ticker) {
        return newsRepository.findTop5ByTickersOrderByPublishedUtcDesc(ticker);
    }
}
