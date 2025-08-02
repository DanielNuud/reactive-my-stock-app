package daniel.nuud.newsservice.service;

import daniel.nuud.newsservice.dto.ApiArticle;
import daniel.nuud.newsservice.dto.ApiResponse;
import daniel.nuud.newsservice.exception.ResourceNotFoundException;
import daniel.nuud.newsservice.model.Article;
import daniel.nuud.newsservice.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private NewsRepository newsRepository;

    @Value("${polygon.api.key}")
    private String apiKey;

    private Mono<ApiResponse> getApiResponse(String ticker) {
        return webClient.get()
                .uri("/v2/reference/news?ticker={ticker}&order=asc&limit=10&sort=published_utc&apiKey={apiKey}",
                        ticker, apiKey)
                .retrieve()
                .bodyToMono(ApiResponse.class)
                .filter(resp -> resp != null && resp.getResults() != null)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("News with ticker " + ticker + " not found")));
    }

    public Mono<Void> fetchAndSaveNews(String ticker) {
        return getApiResponse(ticker)
                .flatMapMany(response -> {
                    if (response == null || response.getResults() == null) {
                        return Flux.error(new ResourceNotFoundException("No news found for ticker " + ticker));
                    }

                    List<String> ids = response.getResults().stream()
                            .map(ApiArticle::getId)
                            .toList();

                    return newsRepository.findAllById(ids)
                            .map(Article::getId)
                            .collect(Collectors.toSet())
                            .flatMapMany(existingIds -> Flux.fromIterable(response.getResults())
                                    .filter(apiArt -> !existingIds.contains(apiArt.getId()))
                                    .map(apiArt -> {
                                        Article article = new Article();
                                        article.setId(apiArt.getId());
                                        article.setTitle(apiArt.getTitle());
                                        article.setAuthor(apiArt.getAuthor());
                                        article.setDescription(apiArt.getDescription());
                                        article.setArticleUrl(apiArt.getArticleUrl());
                                        article.setImageUrl(apiArt.getImageUrl());
                                        article.setTickers(apiArt.getTickers());
                                        article.setPublishedUtc(apiArt.getPublishedUtc());
                                        article.setPublisherName(apiArt.getPublisher().getName());
                                        article.setPublisherLogoUrl(apiArt.getPublisher().getLogoUrl());
                                        article.setPublisherHomepageUrl(apiArt.getPublisher().getHomepageUrl());
                                        article.setPublisherFaviconUrl(apiArt.getPublisher().getFavicon());
                                        return article;
                                    }));
                })
                .collectList()
                .filter(list -> !list.isEmpty())
                .flatMapMany(newsRepository::saveAll)
                .then();
    }

    public Flux<Article> getTop5News(String ticker) {
        return newsRepository.findTop5ByTickersOrderByPublishedUtcDesc(ticker);
    }
}
