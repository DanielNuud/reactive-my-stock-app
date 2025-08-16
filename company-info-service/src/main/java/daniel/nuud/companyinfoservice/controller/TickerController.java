package daniel.nuud.companyinfoservice.controller;

import daniel.nuud.companyinfoservice.exception.ResourceNotFoundException;
import daniel.nuud.companyinfoservice.model.TickerEntity;
import daniel.nuud.companyinfoservice.service.TickerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/tickers")
@RequiredArgsConstructor
@Slf4j
public class TickerController {

    private final TickerService tickerService;

    @GetMapping("/search")
    public Mono<ResponseEntity<List<TickerEntity>>> suggest(@RequestParam("query") String query) {
        return tickerService.tryRefreshTickers(query)
                .flatMap(refreshed -> tickerService.getFromDB(query)
                        .map(list -> ResponseEntity.ok()
                                .header("X-Data-Freshness", refreshed ? "fresh" : "stale")
                                .body(list)))
                .onErrorResume(ResourceNotFoundException.class,
                        ex -> Mono.just(ResponseEntity.notFound().build()));
    }
}
