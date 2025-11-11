package daniel.nuud.companyinfoservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(WebClientConfig.PolygonProps.class)
public class WebClientConfig {

    @Getter
    @Setter
    @ConfigurationProperties(prefix = "external.polygon")
    public static class PolygonProps {
        private String baseUrl;
    }

    @Bean
    ConnectionProvider companyConnectionProvider() {
        return ConnectionProvider.builder("company-http")
                .pendingAcquireMaxCount(10_000)
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .maxConnections(5000)
                .metrics(true)
                .build();
    }

    @Bean
    HttpClient companyHttpClient(ConnectionProvider provider) {
        return HttpClient.create(provider)
                .compress(false)
                .responseTimeout(Duration.ofSeconds(5))
                .metrics(true, s -> s);

    }

    @Bean
    WebClient.Builder companyWebClientBuilder(HttpClient httpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Bean
    public WebClient polygonWebClient(WebClient.Builder webClientBuilder, PolygonProps polygonProps) {
        return webClientBuilder
                .baseUrl(polygonProps.getBaseUrl())
                .build();
    }
}
