package daniel.nuud.currencyservice.controller;

import daniel.nuud.currencyservice.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping("/convert")
    public Mono<Double> getCurrencyConvert(@RequestParam String from,
                                           @RequestParam String to,
                                           @RequestParam Double amount,
                                           @RequestHeader(value = "X-User-Key", defaultValue = "guest") String userKey) {
        return currencyService.convert(from, to, amount, userKey);
    }
}
