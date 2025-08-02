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

    @GetMapping("/{currency}")
    public Mono<Map<String, String>> getCurrency(@PathVariable("currency") String currency) {
        return currencyService.getCurrencyRates(currency.toUpperCase());
    }

    @GetMapping("/convert")
    public Mono<Double> getCurrencyConvert(@RequestParam String from, @RequestParam String to, @RequestParam Double amount) {
        return currencyService.convert(from, to, amount);
    }
}
