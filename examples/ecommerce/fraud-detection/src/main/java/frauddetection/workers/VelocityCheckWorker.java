package frauddetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Real velocity check engine that tracks transaction timestamps per customer
 * and detects suspicious patterns.
 *
 * Velocity checks:
 *   1. Rapid succession: 3+ transactions within 5 minutes
 *   2. Unusual hourly volume: 5+ transactions in the last hour
 *   3. Daily volume spike: 15+ transactions in the last 24 hours
 *   4. Geographic anomaly: transactions from different "regions" in short time
 *      (computed via transactionId hashing as a proxy for IP/device location)
 *
 * Overall velocityResult:
 *   "suspicious" -- 2+ flags raised
 *   "elevated"   -- exactly 1 flag raised
 *   "normal"     -- no flags raised
 *
 * Input: customerId, transactionId
 * Output: velocityResult, transactionsLastHour, transactionsLast24h, velocityFlags
 */
public class VelocityCheckWorker implements Worker {

    /** Transaction timestamps per customer. Thread-safe. */
    private static final ConcurrentHashMap<String, List<Instant>> TRANSACTION_LOG = new ConcurrentHashMap<>();

    /** Transaction "regions" per customer for geographic anomaly detection. */
    private static final ConcurrentHashMap<String, List<Integer>> REGION_LOG = new ConcurrentHashMap<>();

    private static final int RAPID_SUCCESSION_THRESHOLD = 3;
    private static final int RAPID_SUCCESSION_WINDOW_MINUTES = 5;
    private static final int HOURLY_VOLUME_THRESHOLD = 5;
    private static final int DAILY_VOLUME_THRESHOLD = 15;
    private static final int GEO_ANOMALY_REGIONS_THRESHOLD = 3; // distinct regions in 1 hour

    @Override
    public String getTaskDefName() {
        return "frd_velocity_check";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        // --- Validate required inputs ---
        String customerId = (String) task.getInputData().get("customerId");
        if (customerId == null || customerId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: customerId");
            return result;
        }

        String transactionId = (String) task.getInputData().get("transactionId");
        if (transactionId == null || transactionId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: transactionId");
            return result;
        }

        Instant now = Instant.now();

        System.out.println("  [velocity] Checking velocity for customer " + customerId
                + " on transaction " + transactionId);

        // Record this transaction
        List<Instant> timestamps = TRANSACTION_LOG.computeIfAbsent(customerId,
                k -> Collections.synchronizedList(new ArrayList<>()));
        timestamps.add(now);

        // Record the "region" (derived from transaction ID hash as a proxy)
        int region = Math.abs(transactionId.hashCode() % 20);
        List<Integer> regions = REGION_LOG.computeIfAbsent(customerId,
                k -> Collections.synchronizedList(new ArrayList<>()));
        regions.add(region);

        // --- Compute velocity metrics ---
        Instant fiveMinAgo = now.minus(RAPID_SUCCESSION_WINDOW_MINUTES, ChronoUnit.MINUTES);
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        Instant oneDayAgo = now.minus(24, ChronoUnit.HOURS);

        long txInLast5Min;
        long txLastHour;
        long txLast24h;
        synchronized (timestamps) {
            txInLast5Min = timestamps.stream().filter(t -> t.isAfter(fiveMinAgo)).count();
            txLastHour = timestamps.stream().filter(t -> t.isAfter(oneHourAgo)).count();
            txLast24h = timestamps.stream().filter(t -> t.isAfter(oneDayAgo)).count();
        }

        // Check rapid succession
        boolean rapidSuccession = txInLast5Min >= RAPID_SUCCESSION_THRESHOLD;

        // Check unusual hourly volume
        boolean unusualVolume = txLastHour >= HOURLY_VOLUME_THRESHOLD;

        // Check daily volume spike
        boolean dailySpike = txLast24h >= DAILY_VOLUME_THRESHOLD;

        // Check geographic anomaly (distinct regions in last hour)
        long distinctRegionsLastHour;
        synchronized (regions) {
            // Only look at the last N regions corresponding to last hour's transactions
            int recentCount = (int) txLastHour;
            List<Integer> recentRegions = regions.subList(
                    Math.max(0, regions.size() - recentCount), regions.size());
            distinctRegionsLastHour = recentRegions.stream().distinct().count();
        }
        boolean geographicAnomaly = distinctRegionsLastHour >= GEO_ANOMALY_REGIONS_THRESHOLD;

        // --- Compute overall result ---
        int flagCount = (rapidSuccession ? 1 : 0) + (unusualVolume ? 1 : 0)
                + (dailySpike ? 1 : 0) + (geographicAnomaly ? 1 : 0);

        String velocityResult;
        if (flagCount >= 2) {
            velocityResult = "suspicious";
        } else if (flagCount == 1) {
            velocityResult = "elevated";
        } else {
            velocityResult = "normal";
        }

        System.out.println("  [velocity] Customer " + customerId + ": " + velocityResult
                + " (5min=" + txInLast5Min + ", 1h=" + txLastHour + ", 24h=" + txLast24h
                + ", regions=" + distinctRegionsLastHour + ")");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("velocityResult", velocityResult);
        result.getOutputData().put("transactionsLast5Min", (int) txInLast5Min);
        result.getOutputData().put("transactionsLastHour", (int) txLastHour);
        result.getOutputData().put("transactionsLast24h", (int) txLast24h);
        result.getOutputData().put("distinctRegionsLastHour", (int) distinctRegionsLastHour);

        Map<String, Object> velocityFlags = new LinkedHashMap<>();
        velocityFlags.put("rapidSuccession", rapidSuccession);
        velocityFlags.put("unusualVolume", unusualVolume);
        velocityFlags.put("dailySpike", dailySpike);
        velocityFlags.put("geographicAnomaly", geographicAnomaly);
        result.getOutputData().put("velocityFlags", velocityFlags);
        return result;
    }

    /** Resets all in-memory state. Useful for testing. */
    public static void resetState() {
        TRANSACTION_LOG.clear();
        REGION_LOG.clear();
    }
}
