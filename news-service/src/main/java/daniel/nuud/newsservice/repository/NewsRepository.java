package daniel.nuud.newsservice.repository;

import daniel.nuud.newsservice.model.Article;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends ReactiveMongoRepository<Article, String> {
    Optional<Flux<Article>> findAllByTickersContaining(String ticker);
    Flux<Article> findTop5ByTickersOrderByPublishedUtcDesc(String ticker);
}
