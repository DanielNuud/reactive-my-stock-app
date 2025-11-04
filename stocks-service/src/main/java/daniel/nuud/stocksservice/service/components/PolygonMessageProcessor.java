package daniel.nuud.stocksservice.service.components;

import daniel.nuud.stocksservice.model.StockPrice;
import daniel.nuud.stocksservice.service.StocksPriceService;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolygonMessageProcessor {
    private final StocksPriceService stockPriceService;
    private final TenPercentMoveEngine tenPercentMoveEngine;

    public void process(String text) {

        JSONArray arr = new JSONArray(text);

        for (int i = 0; i < arr.length(); i++) {

            JSONObject json = arr.getJSONObject(i);
            String ev = json.optString("ev", "");

            if ("A".equals(ev) || "AM".equals(ev)) {

                String ticker = json.getString("sym");
                double close = json.getDouble("c");
                long ts = json.getLong("e");

                stockPriceService.save(ticker, close, ts);

                tenPercentMoveEngine.onPrice(new StockPrice(ticker, close, ts)).subscribe();

            } else if ("status".equals(ev)) {

                System.out.println("Status: " + json.optString("status")
                        + " | Message: " + json.optString("message"));
            }
        }
    }
}
