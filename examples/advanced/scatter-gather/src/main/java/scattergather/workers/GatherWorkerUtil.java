package scattergather.workers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared utility for gather workers. Computes a deterministic price
 * from the query and source name using a hash function, ensuring
 * reproducible results for the same inputs.
 */
final class GatherWorkerUtil {

    private GatherWorkerUtil() {}

    /**
     * Gathers a price response from a source. The price is deterministic
     * based on the query + source + seed so the same inputs always
     * produce the same output.
     *
     * @param query  the search query
     * @param source the source name
     * @param seed   a seed to vary prices across sources
     * @return response map with source, price, currency, responseTimeMs
     */
    static Map<String, Object> gatherFromSource(String query, String source, int seed) {
        long startNanos = System.nanoTime();

        // Compute a deterministic price based on hash of query + source
        double price = computePrice(query, source, seed);
        long elapsed = (System.nanoTime() - startNanos) / 1_000_000;

        Map<String, Object> response = new HashMap<>();
        response.put("source", source);
        response.put("price", price);
        response.put("currency", "USD");
        response.put("responseTimeMs", elapsed);
        return response;
    }

    /**
     * Computes a deterministic price in the range [10.00, 99.99] using SHA-256.
     */
    private static double computePrice(String query, String source, int seed) {
        try {
            String input = query + "|" + source + "|" + seed;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            // Use first 4 bytes as unsigned int
            int val = ((hash[0] & 0xFF) << 24) | ((hash[1] & 0xFF) << 16)
                    | ((hash[2] & 0xFF) << 8) | (hash[3] & 0xFF);
            // Map to range [10.00, 99.99]
            double normalized = (Math.abs(val) % 9000 + 1000) / 100.0;
            return Math.round(normalized * 100.0) / 100.0;
        } catch (Exception e) {
            return 25.00 + seed * 5.0;
        }
    }
}
