package example;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class MasterRouteSimulation extends Simulation {

    private static final String BASE_URL   = System.getenv().getOrDefault("BASE_URL", "http://api-gateway:8080");
    private static final String WS_BASE_URL= System.getenv().getOrDefault("WS_BASE_URL","ws://api-gateway:8080");

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .wsBaseUrl(WS_BASE_URL)
            .shareConnections()
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling-MasterRoute/Java");

    private static final List<String> TICKERS =
            Arrays.asList("AAPL", "TSLA", "NVDA", "AMZN", "AMD");

    private ChainBuilder chooseTickerOnce =
            exec(session -> {
                String t = TICKERS.get(ThreadLocalRandom.current().nextInt(TICKERS.size()));
                return session.set("ticker", t);
            });

    private ChainBuilder typeAheadSearch =
            repeat(session -> session.getString("ticker").length(), "i").on(
                    exec(session -> {
                        String t = session.getString("ticker");
                        int i = session.getInt("i") + 1;
                        return session.set("q", t.substring(0, i));
                    })
                            .exec(
                                    http("GET tickers search (#{q})")
                                            .get("/api/tickers/search")
                                            .queryParam("query", "#{q}")
                                            .check(status().is(200))
                            )
            );

    private ChainBuilder companyPage =
            exec(
                    http("GET company")
                            .get("/api/companies/#{ticker}")
                            .check(status().is(200))
                            .resources(
                                    http("GET historical one week")
                                            .get("/api/historical/#{ticker}")
                                            .queryParam("period", "one_week")
                                            .check(status().is(200)),
                                    http("GET news")
                                            .get("/api/news/#{ticker}")
                                            .check(status().is(200))
                            )
            )
                    .exec(
                            http("GET historical one month")
                                    .get("/api/historical/#{ticker}")
                                    .queryParam("period", "one_month")
                                    .check(status().is(200))

                    );

    private ChainBuilder notificationsFlow() {
        return exec(
                http("GET notifications")
                        .get("/api/notifications")
                        .queryParam("userKey", "#{userKey}")
                        .check(status().is(200))
                        .check(jmesPath("[0].id").optional().saveAs("notifId"))
        )
                .doIf(session -> session.contains("notifId")).then(
                        exec(
                                http("PATCH notification read")
                                        .patch("/api/notifications/#{notifId}/read")
                                        .check(status().is(204))
                        )
                );
    }

    private ChainBuilder currencyConvertUsdEurDefault() {
        return currencyConvert("USD", "EUR", "100", 3, java.time.Duration.ofMillis(500));
    }

    private ChainBuilder currencyConvert(String from, String to, String amount, int times, java.time.Duration pause) {
        return repeat(times).on(
                exec(
                        http("GET currency convert")
                                .get("/api/currency/convert")
                                .queryParam("from", from)
                                .queryParam("to", to)
                                .queryParam("amount", amount)
                                .check(status().is(200))
                ).pause(pause)
        );
    }

    ScenarioBuilder scn = scenario("Master user journey")
            .exec(session -> session.set("userKey", "user-" + session.userId()).set("period", "one_week"))
            .exec(chooseTickerOnce)

            .group("Search").on(typeAheadSearch)
            .pause(Duration.ofMillis(800))

            .group("CompanyPage").on(companyPage)
            .pause(Duration.ofMillis(300), Duration.ofMillis(800))

            .group("Currency").on(currencyConvertUsdEurDefault())
            .pause(Duration.ofMillis(200), Duration.ofMillis(500))

            .group("Notifications").on(notificationsFlow())
            .pause(Duration.ofMillis(200), Duration.ofMillis(500));

    {
        int START_CONC   = Integer.parseInt(System.getenv().getOrDefault("START_CONC", "0"));
        int TARGET_CONC  = Integer.parseInt(System.getenv().getOrDefault("TARGET_CONC", "1600"));
        int RAMP_MIN     = Integer.parseInt(System.getenv().getOrDefault("RAMP_MIN", "5"));
        int HOLD_MIN     = Integer.parseInt(System.getenv().getOrDefault("HOLD_MIN", "20"));
        int RAMPDOWN_SEC = Integer.parseInt(System.getenv().getOrDefault("RAMPDOWN_SEC", "0"));


        int BASE  = 800;
        int SPIKE = 2500;

        setUp(
                scn.injectClosed(
                        rampConcurrentUsers(0).to(BASE).during(Duration.ofSeconds(60)),
                        constantConcurrentUsers(BASE).during(Duration.ofSeconds(120)),

                        rampConcurrentUsers(BASE).to(SPIKE).during(Duration.ofSeconds(10)),
                        constantConcurrentUsers(SPIKE).during(Duration.ofSeconds(180)),

                        rampConcurrentUsers(SPIKE).to(BASE).during(Duration.ofSeconds(10)),
                        constantConcurrentUsers(BASE).during(Duration.ofSeconds(120))

                )
        )
                .protocols(httpProtocol)
                .assertions(
                        global().failedRequests().percent().lte(1.0),
                        global().responseTime().percentile3().lte(550),
                        global().responseTime().percentile4().lte(900)
                );

//        setUp(
//                scn.injectClosed(
//                        rampConcurrentUsers(START_CONC).to(TARGET_CONC).during(Duration.ofMinutes(RAMP_MIN)),
//                        constantConcurrentUsers(TARGET_CONC).during(Duration.ofMinutes(HOLD_MIN)),
//                        rampConcurrentUsers(TARGET_CONC).to(START_CONC).during(Duration.ofSeconds(RAMPDOWN_SEC))
//                )
//        )
//                .protocols(httpProtocol)
//                .maxDuration(
//                        Duration.ofMinutes(RAMP_MIN + HOLD_MIN)
//                                .plusSeconds(RAMPDOWN_SEC + 60)
//                )
//                .assertions(
//                        global().responseTime().percentile3().lte(3000)
//                );

    }
}
