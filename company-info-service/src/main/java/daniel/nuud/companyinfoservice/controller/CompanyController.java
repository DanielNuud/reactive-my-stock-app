package daniel.nuud.companyinfoservice.controller;

import daniel.nuud.companyinfoservice.model.Company;
import daniel.nuud.companyinfoservice.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
        return companyService.fetchCompany(ticker)
                .map(ResponseEntity::ok);
    }
}
