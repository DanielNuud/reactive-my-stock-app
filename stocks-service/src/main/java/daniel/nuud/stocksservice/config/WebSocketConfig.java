package daniel.nuud.stocksservice.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebSocketConfig {

    @Bean
    public ReactorNettyWebSocketClient reactorNettyWebSocketClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(20))
                .compress(true)
                .keepAlive(true);

        return new ReactorNettyWebSocketClient(httpClient);
    }
}
