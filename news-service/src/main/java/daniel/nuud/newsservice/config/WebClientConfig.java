package daniel.nuud.newsservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

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
    public WebClient polygonWebClient(WebClient.Builder builder, PolygonProps props) {
        return builder
                .baseUrl(props.getBaseUrl())
                .build();
    }
}
