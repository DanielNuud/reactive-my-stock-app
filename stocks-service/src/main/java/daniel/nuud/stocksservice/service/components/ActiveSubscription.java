package daniel.nuud.stocksservice.service.components;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ActiveSubscription {

    // Текущий активный тикер каждого пользователя
    private final ConcurrentHashMap<String, String> currentByUser = new ConcurrentHashMap<>();
    // Счётчик активных пользователей на каждом тикере (для first/last)
    private final ConcurrentHashMap<String, AtomicInteger> countByTicker = new ConcurrentHashMap<>();

    /** Оформить/переключить подписку пользователя на тикер.
     *  @return true — если для этого тикера это первый активный пользователь (0 -> 1). */
    public boolean subscribe(String userKey, String rawTicker) {
        final String user = normUser(userKey);
        final String ticker = norm(rawTicker);

        String previous = currentByUser.put(user, ticker);
        if (previous != null && !previous.equals(ticker)) {
            dec(previous);
        }
        return inc(ticker) == 1;
    }

    /** Отписать пользователя от тикера.
     *  @return true — если это был последний активный пользователь этого тикера (1 -> 0). */
    public boolean unsubscribe(String userKey, String rawTicker) {
        final String user = normUser(userKey);
        final String ticker = norm(rawTicker);

        currentByUser.compute(user, (k, v) -> (v != null && v.equals(ticker)) ? null : v);
        return dec(ticker) == 0;
    }

    /** Активный тикер для конкретного пользователя (использует WS-хендлер). */
    public Optional<String> tickerFor(String userKey) {
        return Optional.ofNullable(currentByUser.get(normUser(userKey)));
    }

    /** Сколько сейчас активных пользователей у тикера (по REST удобно для логов). */
    public int subscribers(String rawTicker) {
        AtomicInteger c = countByTicker.get(norm(rawTicker));
        return c == null ? 0 : c.get();
    }

    /** Текущие пользователи, подписанные на тикер (если вдруг понадобится). */
    public Set<String> usersOf(String rawTicker) {
        // Лёгкая выборка через currentByUser (O(n)); при желании можно хранить обратную мапу.
        final String t = norm(rawTicker);
        return currentByUser.entrySet().stream()
                .filter(e -> t.equals(e.getValue()))
                .map(java.util.Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    // --- helpers ---

    private int inc(String ticker) {
        return countByTicker.computeIfAbsent(ticker, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private int dec(String ticker) {
        AtomicInteger c = countByTicker.get(ticker);
        if (c == null) return 0;
        int val = c.decrementAndGet();
        if (val <= 0) {
            countByTicker.remove(ticker, c);
            return 0;
        }
        return val;
    }

    private String norm(String t) { return t == null ? "" : t.trim().toUpperCase(); }
    private String normUser(String u) { return (u == null || u.isBlank()) ? "guest" : u.trim(); }
}
