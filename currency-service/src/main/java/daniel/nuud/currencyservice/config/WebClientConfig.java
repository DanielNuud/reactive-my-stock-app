package daniel.nuud.currencyservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(@Value("${freecurrency.api.key}") String apiKey) {
        return WebClient.builder()
                .baseUrl("https://api.freecurrencyapi.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultUriVariables(Map.of("apiKey", apiKey))
                .build();
    }
}
