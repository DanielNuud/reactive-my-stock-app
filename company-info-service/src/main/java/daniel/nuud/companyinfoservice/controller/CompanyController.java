package daniel.nuud.companyinfoservice.controller;

import daniel.nuud.companyinfoservice.exception.ResourceNotFoundException;
import daniel.nuud.companyinfoservice.model.Company;
import daniel.nuud.companyinfoservice.service.CompanyService;
import daniel.nuud.companyinfoservice.service.TickerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/{ticker}")
    public Mono<ResponseEntity<Company>> getCompany(@PathVariable String ticker) {
        log.info("Received GET /api/companies/{}", ticker);

        return companyService.getFromDB(ticker)
                .onErrorResume(ResourceNotFoundException.class, ex ->
                        companyService.tryRefreshCompany(ticker)
                                .onErrorResume(e -> Mono.empty())
                )
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/{ticker}/db")
    public Mono<ResponseEntity<Company>> getOnlyFromDb(@PathVariable String ticker) {
        return companyService.getFromDB(ticker)
                .map(ResponseEntity::ok)
                .onErrorResume(ResourceNotFoundException.class,
                        ex -> Mono.just(ResponseEntity.notFound().build()));
    }
}
