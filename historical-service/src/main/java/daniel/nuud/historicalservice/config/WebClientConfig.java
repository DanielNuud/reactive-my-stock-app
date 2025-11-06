package daniel.nuud.historicalservice.config;

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
    ConnectionProvider histConnectionProvider() {
        return ConnectionProvider.builder("hist-http")
                .maxConnections(2000)                       // под 1–1.6k VU
                .pendingAcquireMaxCount(4096)               // очередь ожидания
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .evictInBackground(Duration.ofSeconds(30))
                .lifo()
                .metrics(true)
                .build();
    }

    // 2) HttpClient с таймаутами
    @Bean
    HttpClient histHttpClient(ConnectionProvider provider) {
        return HttpClient.create(provider)
                .compress(true)
                .responseTimeout(Duration.ofSeconds(15))         // было 5 — мало для аггрегатов
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .doOnConnected(c -> c
                        .addHandlerLast(new ReadTimeoutHandler(0))   // убери дублирующий socket timeout
                        .addHandlerLast(new WriteTimeoutHandler(0))); // socket write
    }

    // 3) Общий builder с нашим коннектором
    @Bean
    WebClient.Builder histWebClientBuilder(HttpClient httpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Connection", "keep-alive");
    }

    @Bean
    public WebClient polygonWebClient(WebClient.Builder builder, PolygonProps props) {
        return builder
                .baseUrl(props.getBaseUrl())
                .build();
    }

    @Bean
    public WebClient notificationWebClient() {
        return WebClient.builder()
                .baseUrl("http://notification-service:8080")
                .build();
    }
}
