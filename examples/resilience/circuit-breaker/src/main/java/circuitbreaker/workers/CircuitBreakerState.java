package circuitbreaker.workers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Persistent circuit breaker state that survives across worker invocations.
 * Uses a file-backed store (in java.io.tmpdir) alongside an in-memory cache.
 *
 * State transitions:
 *   CLOSED -> OPEN (when failureCount >= threshold)
 *   OPEN -> HALF_OPEN (after cooldown period)
 *   HALF_OPEN -> CLOSED (on success) or OPEN (on failure)
 */
public final class CircuitBreakerState {

    private static final Path STATE_DIR = Path.of(System.getProperty("java.io.tmpdir"), "circuit-breaker-state");
    private static final ConcurrentHashMap<String, String> STATE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> FAILURE_COUNTS = new ConcurrentHashMap<>();

    private CircuitBreakerState() {}

    /** Persist state for a service. */
    public static void setState(String serviceName, String state) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalStateException("serviceName cannot be null or blank");
        }
        if (state == null || state.isBlank()) {
            throw new IllegalStateException("state cannot be null or blank");
        }
        STATE_CACHE.put(serviceName, state);
        try {
            Files.createDirectories(STATE_DIR);
            Path stateFile = STATE_DIR.resolve(sanitize(serviceName) + ".state");
            Files.writeString(stateFile, state + "\n" + Instant.now(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            // File persistence is best-effort; in-memory cache is primary
            System.err.println("Warning: Could not persist circuit state to file: " + e.getMessage());
        }
    }

    /** Get state for a service, loading from file if not in cache. */
    public static String getState(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) return null;

        String cached = STATE_CACHE.get(serviceName);
        if (cached != null) return cached;

        // Try to load from file
        try {
            Path stateFile = STATE_DIR.resolve(sanitize(serviceName) + ".state");
            if (Files.exists(stateFile)) {
                String content = Files.readString(stateFile).trim();
                String state = content.split("\n")[0];
                STATE_CACHE.put(serviceName, state);
                return state;
            }
        } catch (IOException e) {
            // Fall through
        }
        return null;
    }

    /** Increment and return the failure count for a service. */
    public static int incrementFailureCount(String serviceName) {
        return FAILURE_COUNTS
                .computeIfAbsent(serviceName, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    /** Get the current failure count for a service. */
    public static int getFailureCount(String serviceName) {
        AtomicInteger count = FAILURE_COUNTS.get(serviceName);
        return count != null ? count.get() : 0;
    }

    /** Reset the failure count for a service. */
    public static void resetFailureCount(String serviceName) {
        FAILURE_COUNTS.remove(serviceName);
    }

    /** Clear all state. For testing only. */
    public static void clearAll() {
        STATE_CACHE.clear();
        FAILURE_COUNTS.clear();
        try {
            if (Files.exists(STATE_DIR)) {
                try (var walk = Files.list(STATE_DIR)) {
                    walk.filter(p -> p.toString().endsWith(".state"))
                            .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
                }
            }
        } catch (IOException ignored) {}
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
