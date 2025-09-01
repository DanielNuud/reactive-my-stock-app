package daniel.nuud.stocksservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient polygonWebClient(@Value("${POLYGON_API_KEY}") String apiKey) {
        return WebClient.builder()
                .baseUrl("https://api.polygon.io")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    @Bean
    public WebClient notificationWebClient() {
        return WebClient.builder()
                .baseUrl("http://notification-service:8080")
                .build();
    }

    @Bean
    public WebClient historicalWebClient() {
        return WebClient.builder()
                .baseUrl("http://historical-service:8080")
                .build();
    }


    @Bean
    public WebClient currencyWebClient() {
        return WebClient.builder()
                .baseUrl("http://currency-service:8080")
                .build();
    }
}
