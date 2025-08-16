package daniel.nuud.newsservice.controller;

import daniel.nuud.newsservice.exception.ResourceNotFoundException;
import daniel.nuud.newsservice.model.Article;
import daniel.nuud.newsservice.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/{ticker}")
    public Mono<ResponseEntity<List<Article>>> fetchNews(@PathVariable String ticker) {
        return newsService.tryRefreshNews(ticker)
                .flatMap(refreshed -> newsService.getTop5NewsByTicker(ticker)
                        .map(list -> ResponseEntity.ok()
                                .header("X-Data-Freshness", refreshed ? "fresh" : "stale")
                                .body(list)))
                .onErrorResume(ResourceNotFoundException.class,
                        ex -> Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/{ticker}/top/db")
    public Mono<ResponseEntity<List<Article>>> topFromDb(@PathVariable String ticker) {
        return newsService.getTop5NewsByTicker(ticker)
                .map(ResponseEntity::ok)
                .onErrorResume(ResourceNotFoundException.class,
                        ex -> Mono.just(ResponseEntity.notFound().build()));
    }

}

