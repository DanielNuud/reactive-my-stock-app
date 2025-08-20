package daniel.nuud.stocksservice.service.components;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ExponentialBackoff {

    private final AtomicInteger attempts = new AtomicInteger(0);
    private final long maxMs = 30_000;

    public long nextDelayMs() {

        int a = attempts.incrementAndGet();
        long base = (long) (Math.pow(2, a) * 500);
        long jitter = ThreadLocalRandom.current().nextLong(250, 1000);

        return Math.min(maxMs, base) + jitter;
    }

    public void reset() {
        attempts.set(0);
    }
}

