package daniel.nuud.newsservice.controller;

import daniel.nuud.newsservice.model.Article;
import daniel.nuud.newsservice.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/{ticker}")
    public Mono<ResponseEntity<Flux<Article>>> fetchNews(@PathVariable String ticker) {
        return newsService.fetchAndSaveNews(ticker)
                .thenReturn(ResponseEntity.ok(newsService.getTop5News(ticker)));
    }

//    @GetMapping
//    public Mono<ResponseEntity<Flux<Article>>> getAllNews() {
//        return Mono.just(ResponseEntity.ok(newsService.getAllNews()));
//    }
}

