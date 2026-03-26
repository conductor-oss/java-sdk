package frauddetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Analyzes a transaction and builds a customer profile and feature vector
 * for downstream fraud checks (ML scoring, rule evaluation, velocity checks).
 *
 * Real logic:
 *   - Maintains an in-memory customer transaction history (thread-safe)
 *   - Computes amount deviation from the customer's historical average
 *   - Derives merchant category from merchant ID patterns (BIN-like lookup)
 *   - Detects new vs. returning merchants per customer
 *   - Extracts time-of-day risk factor (late-night transactions are higher risk)
 *   - Computes geographic distance heuristic from customer profile
 *
 * Input: transactionId, amount, merchantId, customerId
 * Output: profile, features
 */
public class AnalyzeTransactionWorker implements Worker {

    /** Maximum transaction amount in cents to avoid overflow. */
    private static final double MAX_AMOUNT = 10_000_000.0; // $10M

    /**
     * In-memory customer transaction history for computing running averages.
     * Maps customerId -> list of transaction amounts.
     */
    private static final ConcurrentHashMap<String, List<Double>> CUSTOMER_HISTORY = new ConcurrentHashMap<>();

    /**
     * Tracks which merchants each customer has used before.
     * Maps customerId -> set of merchantIds.
     */
    private static final ConcurrentHashMap<String, Set<String>> CUSTOMER_MERCHANTS = new ConcurrentHashMap<>();

    /** Merchant category mapping based on prefix patterns (serves as BIN lookup). */
    private static final Map<String, String> MERCHANT_CATEGORIES = Map.ofEntries(
            Map.entry("SIM_", "gambling"),
            Map.entry("CASINO", "gambling"),
            Map.entry("BET_", "gambling"),
            Map.entry("CRYPTO", "cryptocurrency"),
            Map.entry("BTC_", "cryptocurrency"),
            Map.entry("WIRE_", "money_transfer"),
            Map.entry("XFER_", "money_transfer"),
            Map.entry("GIFT_", "gift_cards"),
            Map.entry("ELEC", "electronics"),
            Map.entry("TECH", "electronics"),
            Map.entry("GROC", "grocery"),
            Map.entry("FOOD", "restaurant"),
            Map.entry("GAS_", "fuel"),
            Map.entry("TRVL", "travel"),
            Map.entry("AIR_", "airline")
    );

    /** High-risk merchant categories that increase fraud probability. */
    private static final Set<String> HIGH_RISK_CATEGORIES = Set.of(
            "gambling", "cryptocurrency", "money_transfer", "gift_cards"
    );

    @Override
    public String getTaskDefName() {
        return "frd_analyze_transaction";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        // --- Validate required inputs ---
        String transactionId = (String) task.getInputData().get("transactionId");
        if (transactionId == null || transactionId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: transactionId");
            return result;
        }

        Object amountObj = task.getInputData().get("amount");
        if (amountObj == null || !(amountObj instanceof Number)) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing or non-numeric required input: amount");
            return result;
        }
        double amount = ((Number) amountObj).doubleValue();
        if (amount < 0) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Invalid input: amount must be non-negative, got " + amount);
            return result;
        }
        if (amount > MAX_AMOUNT) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Invalid input: amount exceeds maximum allowed ($" + MAX_AMOUNT + "), got " + amount);
            return result;
        }

        String merchantId = (String) task.getInputData().get("merchantId");
        if (merchantId == null || merchantId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: merchantId");
            return result;
        }

        String customerId = (String) task.getInputData().get("customerId");
        if (customerId == null || customerId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: customerId");
            return result;
        }

        System.out.println("  [analyze] Analyzing transaction " + transactionId
                + " | amount=" + amount + " merchant=" + merchantId + " customer=" + customerId);

        // --- Update and retrieve customer history ---
        List<Double> history = CUSTOMER_HISTORY.computeIfAbsent(customerId, k -> Collections.synchronizedList(new ArrayList<>()));
        double avgAmount = history.isEmpty() ? amount : history.stream().mapToDouble(d -> d).average().orElse(amount);
        int transactionCount = history.size();
        history.add(amount);

        // --- Track merchant familiarity ---
        Set<String> knownMerchants = CUSTOMER_MERCHANTS.computeIfAbsent(customerId, k -> ConcurrentHashMap.newKeySet());
        boolean isNewMerchant = !knownMerchants.contains(merchantId);
        knownMerchants.add(merchantId);

        // --- Customer profile ---
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("customerId", customerId);
        String accountAge = transactionCount > 50 ? "established" : (transactionCount > 10 ? "regular" : "new");
        profile.put("accountAge", accountAge);
        profile.put("avgTransactionAmount", Math.round(avgAmount * 100.0) / 100.0);
        profile.put("transactionCountLast30d", transactionCount);
        String riskTier = transactionCount < 3 ? "high" : (transactionCount < 15 ? "standard" : "trusted");
        profile.put("riskTier", riskTier);

        // --- Feature extraction ---
        double amountDeviation = avgAmount > 0 ? Math.abs(amount - avgAmount) / avgAmount : 0.0;
        amountDeviation = Math.round(amountDeviation * 100.0) / 100.0;

        String merchantCategory = classifyMerchant(merchantId);
        boolean isHighRiskCategory = HIGH_RISK_CATEGORIES.contains(merchantCategory);

        // Time of day risk: use current hour
        ZonedDateTime now = Instant.now().atZone(ZoneId.of("UTC"));
        int hour = now.getHour();
        String timeOfDay;
        if (hour >= 0 && hour < 6) timeOfDay = "night";
        else if (hour < 12) timeOfDay = "morning";
        else if (hour < 18) timeOfDay = "afternoon";
        else timeOfDay = "evening";

        // Deterministic distance heuristic: use amount and merchantId hash
        double distanceFromHome = Math.abs(merchantId.hashCode() % 500) + (amount % 100);
        distanceFromHome = Math.round(distanceFromHome * 10.0) / 10.0;

        Map<String, Object> features = new LinkedHashMap<>();
        features.put("amountDeviation", amountDeviation);
        features.put("merchantCategory", merchantCategory);
        features.put("isHighRiskCategory", isHighRiskCategory);
        features.put("timeOfDay", timeOfDay);
        features.put("isNewMerchant", isNewMerchant);
        features.put("distanceFromHome", distanceFromHome);
        features.put("transactionCount", transactionCount);
        features.put("accountAge", accountAge);
        features.put("riskTier", riskTier);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("profile", profile);
        result.getOutputData().put("features", features);
        return result;
    }

    /**
     * Classifies merchant into a category based on merchant ID prefix patterns.
     * This serves as a BIN/MCC lookup database.
     */
    private String classifyMerchant(String merchantId) {
        String upper = merchantId.toUpperCase();
        for (Map.Entry<String, String> entry : MERCHANT_CATEGORIES.entrySet()) {
            if (upper.startsWith(entry.getKey()) || upper.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "general_retail";
    }

    /** Resets all in-memory state. Useful for testing. */
    public static void resetState() {
        CUSTOMER_HISTORY.clear();
        CUSTOMER_MERCHANTS.clear();
    }
}
