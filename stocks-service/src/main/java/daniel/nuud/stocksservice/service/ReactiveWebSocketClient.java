package daniel.nuud.stocksservice.service;

import daniel.nuud.stocksservice.model.StockPrice;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;

@Component
public class ReactiveWebSocketClient {

    private final Sinks.Many<StockPrice> sink = Sinks.many().multicast().onBackpressureBuffer();

    public Flux<StockPrice> connect(String url, String apiKey) {
        ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();

        client.execute(
                URI.create(url),
                session -> {
                    session.send(Mono.just(session.textMessage("{\"action\":\"auth\",\"params\":\"" + apiKey + "\"}")))
                            .subscribe();

                    return session.receive()
                            .map(msg -> msg.getPayloadAsText())
                            .doOnNext(this::handleTextMessage)
                            .then();
                }
        ).subscribe();

        return sink.asFlux();
    }

    private void handleTextMessage(String text) {
        try {
            JSONArray array = new JSONArray(text);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.getJSONObject(i);
                String event = json.optString("ev");

                if ("AM".equals(event)) {
                    String ticker = json.getString("sym");
                    double close = json.getDouble("c");
                    long timestamp = json.getLong("e");

                    sink.tryEmitNext(new StockPrice(ticker, close, timestamp));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing message: " + text);
            e.printStackTrace();
        }
    }
}
