package daniel.nuud.stocksservice.service.components;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ActiveSubscription {
    private final AtomicReference<String> currentTicker = new AtomicReference<>(null);
    private final AtomicReference<String> ownerUserKey  = new AtomicReference<>(null);

    public void set(String userKey, String ticker) {
        ownerUserKey.set(userKey);
        currentTicker.set(ticker.toUpperCase());
    }

    public void clearIfTicker(String ticker) {
        String t = ticker == null ? null : ticker.toUpperCase();
        if (t != null && t.equals(currentTicker.get())) {
            currentTicker.set(null);
            ownerUserKey.set(null);
        }
    }

    public Optional<String> owner()  { return Optional.ofNullable(ownerUserKey.get()); }
    public Optional<String> ticker() { return Optional.ofNullable(currentTicker.get()); }
}
