package daniel.nuud.newsservice.config;

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
import java.util.Map;

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
    ConnectionProvider newsConnectionProvider() {
        return ConnectionProvider.builder("news-http")
                .maxConnections(2000)
                .metrics(true)
                .build();
    }

    @Bean
    HttpClient newsHttpClient(ConnectionProvider provider) {
        return HttpClient.create(provider)
                .compress(false)
                .metrics(true, s -> s);
    }

    @Bean
    WebClient.Builder newsWebClientBuilder(HttpClient httpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Bean
    public WebClient polygonWebClient(WebClient.Builder builder, PolygonProps props) {
        return builder
                .baseUrl(props.getBaseUrl())
                .build();
    }
}
