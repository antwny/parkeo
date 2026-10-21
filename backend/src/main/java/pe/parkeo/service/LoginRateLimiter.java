package pe.parkeo.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter simple en memoria para el login.
 * Bloquea un usuario tras N intentos fallidos dentro de la ventana de tiempo.
 * Para producción se recomienda mover esto a Redis/Cloudflare.
 */
@Service
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final java.time.Duration WINDOW = java.time.Duration.ofMinutes(15);
    private static final int MAX_KEYS = 10_000;

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public boolean isBlocked(String emailKey) {
        Deque<Instant> attempts = failures.get(emailKey);
        if (attempts == null || attempts.isEmpty()) {
            return false;
        }
        purge(emailKey, attempts);
        return attempts.size() >= MAX_ATTEMPTS;
    }

    public void recordFailure(String emailKey) {
        if (failures.size() > MAX_KEYS) {
            failures.clear(); // protege de crecimiento sin límite
        }
        Deque<Instant> attempts = failures.computeIfAbsent(emailKey, k -> new ArrayDeque<>());
        purge(emailKey, attempts);
        attempts.addLast(Instant.now());
    }

    public void reset(String emailKey) {
        failures.remove(emailKey);
    }

    private void purge(String emailKey, Deque<Instant> attempts) {
        Instant cutoff = Instant.now().minus(WINDOW);
        boolean changed = attempts.removeIf(at -> at.isBefore(cutoff));
        if (changed && attempts.isEmpty()) {
            failures.remove(emailKey);
        }
    }
}
