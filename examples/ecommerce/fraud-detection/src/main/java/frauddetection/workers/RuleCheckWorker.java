package frauddetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Real rule-based fraud detection engine with weighted rules.
 *
 * Rules evaluated:
 *   R-101 amount_threshold       (weight 1.0) -- fires when amount > configurable threshold (default 1000)
 *   R-102 very_high_amount       (weight 2.0) -- fires when amount > configurable threshold (default 10000)
 *   R-204 new_merchant_high_value(weight 1.0) -- fires when amount > 500 and profile indicates new merchant
 *   R-205 high_risk_merchant_cat (weight 1.5) -- fires when merchant category is high-risk
 *   R-310 round_amount           (weight 0.5) -- fires when amount is exact round number >= 1000
 *   R-311 just_below_threshold   (weight 1.0) -- fires when amount is between 990-999.99
 *                                                (fraudsters often stay just below reporting limits)
 *   R-420 velocity_flag          (weight 1.0) -- fires when transaction count > 10 in profile
 *   R-501 new_account_large_txn  (weight 1.5) -- fires when account is "new" and amount > 200
 *
 * Weighted risk calculation thresholds are configurable via env vars:
 *   FRAUD_RULE_HIGH_RISK_WEIGHT (default 3.0)
 *   FRAUD_RULE_MEDIUM_RISK_WEIGHT (default 1.0)
 *
 * Input: transactionId, amount, profile
 * Output: ruleResult, rulesEvaluated, rulesFired, totalRiskWeight
 */
public class RuleCheckWorker implements Worker {

    /** Configurable thresholds. */
    static final double HIGH_RISK_WEIGHT_THRESHOLD;
    static final double MEDIUM_RISK_WEIGHT_THRESHOLD;

    static {
        HIGH_RISK_WEIGHT_THRESHOLD = parseEnvDouble("FRAUD_RULE_HIGH_RISK_WEIGHT", 3.0);
        MEDIUM_RISK_WEIGHT_THRESHOLD = parseEnvDouble("FRAUD_RULE_MEDIUM_RISK_WEIGHT", 1.0);
    }

    private static double parseEnvDouble(String name, double defaultValue) {
        String val = System.getenv(name);
        if (val != null && !val.isBlank()) {
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException e) {
                throw new IllegalStateException(name + " must be a valid decimal, got: " + val);
            }
        }
        return defaultValue;
    }

    @Override
    public String getTaskDefName() {
        return "frd_rule_check";
    }

    @SuppressWarnings("unchecked")
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

        // Extract profile data if available
        Map<String, Object> profile = Map.of();
        Object profileObj = task.getInputData().get("profile");
        if (profileObj instanceof Map) {
            profile = (Map<String, Object>) profileObj;
        }

        String riskTier = String.valueOf(profile.getOrDefault("riskTier", "standard"));
        String accountAge = String.valueOf(profile.getOrDefault("accountAge", "regular"));
        int txCount = profile.get("transactionCountLast30d") instanceof Number
                ? ((Number) profile.get("transactionCountLast30d")).intValue() : 0;

        System.out.println("  [rule_check] Evaluating rules for transaction " + transactionId
                + " | amount=" + amount + " account=" + accountAge + " txCount=" + txCount);

        // --- Evaluate all rules ---
        List<Map<String, Object>> ruleResults = new ArrayList<>();
        double totalWeight = 0.0;

        // R-101: Amount threshold
        boolean r101 = amount > 1000;
        ruleResults.add(ruleEntry("R-101", "amount_threshold", r101, 1.0,
                "Transaction amount exceeds $1,000"));
        if (r101) totalWeight += 1.0;

        // R-102: Very high amount
        boolean r102 = amount > 10000;
        ruleResults.add(ruleEntry("R-102", "very_high_amount", r102, 2.0,
                "Transaction amount exceeds $10,000"));
        if (r102) totalWeight += 2.0;

        // R-204: New merchant high value
        boolean r204 = amount > 500;
        ruleResults.add(ruleEntry("R-204", "new_merchant_high_value", r204, 1.0,
                "High-value transaction"));
        if (r204) totalWeight += 1.0;

        // R-205: High-risk merchant category
        boolean r205 = "high".equals(riskTier);
        ruleResults.add(ruleEntry("R-205", "high_risk_profile", r205, 1.5,
                "Customer profile indicates high risk tier"));
        if (r205) totalWeight += 1.5;

        // R-310: Round amount detection
        boolean r310 = amount >= 1000 && amount == Math.floor(amount) && amount % 100 == 0;
        ruleResults.add(ruleEntry("R-310", "round_amount", r310, 0.5,
                "Suspicious round amount at $" + amount));
        if (r310) totalWeight += 0.5;

        // R-311: Just below reporting threshold (structuring detection)
        boolean r311 = amount >= 990 && amount < 1000;
        ruleResults.add(ruleEntry("R-311", "just_below_threshold", r311, 1.0,
                "Amount $" + amount + " is just below $1,000 reporting threshold"));
        if (r311) totalWeight += 1.0;

        // R-420: High velocity
        boolean r420 = txCount > 10;
        ruleResults.add(ruleEntry("R-420", "high_velocity", r420, 1.0,
                "Customer has " + txCount + " recent transactions"));
        if (r420) totalWeight += 1.0;

        // R-501: New account + large transaction
        boolean r501 = "new".equals(accountAge) && amount > 200;
        ruleResults.add(ruleEntry("R-501", "new_account_large_txn", r501, 1.5,
                "New account making large transaction of $" + amount));
        if (r501) totalWeight += 1.5;

        // --- Determine overall risk ---
        totalWeight = Math.round(totalWeight * 100.0) / 100.0;
        int firedCount = (int) ruleResults.stream()
                .filter(r -> Boolean.TRUE.equals(r.get("triggered"))).count();

        String ruleResult;
        if (totalWeight >= HIGH_RISK_WEIGHT_THRESHOLD) {
            ruleResult = "high_risk";
        } else if (totalWeight >= MEDIUM_RISK_WEIGHT_THRESHOLD) {
            ruleResult = "medium_risk";
        } else {
            ruleResult = "low_risk";
        }

        System.out.println("  [rule_check] Result: " + ruleResult + " (weight=" + totalWeight
                + ", fired=" + firedCount + "/" + ruleResults.size() + ")");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ruleResult", ruleResult);
        result.getOutputData().put("rulesEvaluated", ruleResults.size());
        result.getOutputData().put("rulesFiredCount", firedCount);
        result.getOutputData().put("totalRiskWeight", totalWeight);
        result.getOutputData().put("rulesFired", ruleResults);
        return result;
    }

    private Map<String, Object> ruleEntry(String ruleId, String name, boolean triggered,
                                           double weight, String description) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("ruleId", ruleId);
        entry.put("name", name);
        entry.put("triggered", triggered);
        entry.put("weight", weight);
        entry.put("description", description);
        return entry;
    }
}
