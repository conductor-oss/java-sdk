package frauddetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Makes the final fraud decision by combining rule check, ML score, and velocity check results
 * using a weighted ensemble approach.
 *
 * Decision logic:
 *   1. Compute a combined risk score: 0.4 * mlScore + 0.3 * ruleScore + 0.3 * velocityScore
 *   2. Apply override rules:
 *      - Instant BLOCK if ML score > 0.8 AND rules = high_risk
 *      - Instant BLOCK if velocity = suspicious AND ML score > 0.5
 *   3. Apply configurable thresholds:
 *      BLOCK   -- combined score > blockThreshold or any override triggered
 *      REVIEW  -- combined score > reviewThreshold
 *      APPROVE -- combined score <= reviewThreshold
 *
 * Input: transactionId, ruleResult, mlScore, velocityResult
 * Output: decision, riskScore, reason, signals
 */
public class DecideWorker implements Worker {

    /** Configurable via environment variables. */
    public static final double BLOCK_THRESHOLD;
    public static final double REVIEW_THRESHOLD;

    static {
        BLOCK_THRESHOLD = parseEnvDouble("FRAUD_BLOCK_THRESHOLD", 0.7);
        REVIEW_THRESHOLD = parseEnvDouble("FRAUD_REVIEW_THRESHOLD", 0.4);
    }

    private static double parseEnvDouble(String name, double defaultValue) {
        String val = System.getenv(name);
        if (val != null && !val.isBlank()) {
            try {
                double parsed = Double.parseDouble(val);
                if (parsed < 0 || parsed > 1.0) {
                    throw new IllegalStateException(name + " must be between 0 and 1.0, got " + parsed);
                }
                return parsed;
            } catch (NumberFormatException e) {
                throw new IllegalStateException(name + " must be a valid decimal, got: " + val);
            }
        }
        return defaultValue;
    }

    @Override
    public String getTaskDefName() {
        return "frd_decide";
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

        String ruleResult = (String) task.getInputData().get("ruleResult");
        if (ruleResult == null || ruleResult.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: ruleResult");
            return result;
        }

        Object mlScoreObj = task.getInputData().get("mlScore");
        if (mlScoreObj == null || !(mlScoreObj instanceof Number)) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing or non-numeric required input: mlScore");
            return result;
        }
        double mlScore = ((Number) mlScoreObj).doubleValue();
        if (mlScore < 0 || mlScore > 1.0) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Invalid input: mlScore must be between 0 and 1.0, got " + mlScore);
            return result;
        }

        String velocityResult = (String) task.getInputData().get("velocityResult");
        if (velocityResult == null || velocityResult.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: velocityResult");
            return result;
        }

        System.out.println("  [decide] Transaction " + transactionId
                + " | rules=" + ruleResult + " ml=" + mlScore + " velocity=" + velocityResult);

        // --- Convert categorical signals to numeric scores ---
        double ruleScore;
        switch (ruleResult) {
            case "high_risk": ruleScore = 1.0; break;
            case "medium_risk": ruleScore = 0.5; break;
            case "low_risk": ruleScore = 0.0; break;
            default:
                result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
                result.setReasonForIncompletion("Invalid ruleResult value: '" + ruleResult
                        + "'. Must be one of: high_risk, medium_risk, low_risk");
                return result;
        }

        double velocityScore;
        switch (velocityResult) {
            case "suspicious": velocityScore = 1.0; break;
            case "elevated": velocityScore = 0.5; break;
            case "normal": velocityScore = 0.0; break;
            default:
                result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
                result.setReasonForIncompletion("Invalid velocityResult value: '" + velocityResult
                        + "'. Must be one of: suspicious, elevated, normal");
                return result;
        }

        // --- Weighted ensemble score ---
        double combinedScore = 0.4 * mlScore + 0.3 * ruleScore + 0.3 * velocityScore;
        combinedScore = Math.round(combinedScore * 100.0) / 100.0;

        // --- Collect risk signals ---
        List<String> signals = new ArrayList<>();
        if (mlScore > 0.5) signals.add("High ML fraud score (" + mlScore + ")");
        if ("high_risk".equals(ruleResult)) signals.add("High-risk rules triggered");
        if ("medium_risk".equals(ruleResult)) signals.add("Medium-risk rules triggered");
        if ("suspicious".equals(velocityResult)) signals.add("Suspicious transaction velocity");
        if ("elevated".equals(velocityResult)) signals.add("Elevated transaction velocity");

        // --- Check override conditions ---
        boolean overrideBlock = false;
        if (mlScore > 0.8 && "high_risk".equals(ruleResult)) {
            overrideBlock = true;
            signals.add("OVERRIDE: ML score > 0.8 combined with high-risk rules");
        }
        if ("suspicious".equals(velocityResult) && mlScore > 0.5) {
            overrideBlock = true;
            signals.add("OVERRIDE: Suspicious velocity combined with elevated ML score");
        }

        // --- Final decision ---
        String decision;
        double riskScore;
        String reason;

        if (overrideBlock || combinedScore > BLOCK_THRESHOLD) {
            decision = "BLOCK";
            riskScore = Math.max(combinedScore, 0.85);
            reason = overrideBlock
                    ? "Blocked by override rule: multiple high-risk signals detected"
                    : "Combined risk score " + combinedScore + " exceeds block threshold";
        } else if (combinedScore > REVIEW_THRESHOLD) {
            decision = "REVIEW";
            riskScore = combinedScore;
            reason = "Combined risk score " + combinedScore + " requires manual review";
        } else {
            decision = "APPROVE";
            riskScore = combinedScore;
            reason = "All checks within acceptable thresholds (combined score: " + combinedScore + ")";
        }

        System.out.println("  [decide] Decision: " + decision + " (combined=" + combinedScore
                + ", risk=" + riskScore + ")");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("decision", decision);
        result.getOutputData().put("riskScore", riskScore);
        result.getOutputData().put("combinedScore", combinedScore);
        result.getOutputData().put("reason", reason);
        result.getOutputData().put("signals", signals);

        Map<String, Object> componentScores = new LinkedHashMap<>();
        componentScores.put("mlScore", mlScore);
        componentScores.put("ruleScore", ruleScore);
        componentScores.put("velocityScore", velocityScore);
        result.getOutputData().put("componentScores", componentScores);
        return result;
    }
}
