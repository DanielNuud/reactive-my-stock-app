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
        log.info("GET /api/companies/{}", ticker);
        return companyService.getOrRefresh(ticker)
                .map(ResponseEntity::ok);
    }
}
