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
 *   3. Apply thresholds:
 *      BLOCK   — combined score > 0.7 or any override triggered
 *      REVIEW  — combined score > 0.4
 *      APPROVE — combined score <= 0.4
 *
 * Input: transactionId, ruleResult, mlScore, velocityResult
 * Output: decision, riskScore, reason, signals
 */
public class DecideWorker implements Worker {

    public static final double BLOCK_THRESHOLD = 0.7;
    public static final double REVIEW_THRESHOLD = 0.4;

    @Override
    public String getTaskDefName() {
        return "frd_decide";
    }

    @Override
    public TaskResult execute(Task task) {
        String transactionId = (String) task.getInputData().get("transactionId");
        if (transactionId == null) transactionId = "UNKNOWN";
        String ruleResult = (String) task.getInputData().get("ruleResult");
        if (ruleResult == null) ruleResult = "low_risk";
        Object mlScoreObj = task.getInputData().get("mlScore");
        String velocityResult = (String) task.getInputData().get("velocityResult");
        if (velocityResult == null) velocityResult = "normal";

        double mlScore = 0.0;
        if (mlScoreObj instanceof Number) {
            mlScore = ((Number) mlScoreObj).doubleValue();
        }

        System.out.println("  [decide] Transaction " + transactionId
                + " | rules=" + ruleResult + " ml=" + mlScore + " velocity=" + velocityResult);

        // --- Convert categorical signals to numeric scores ---
        double ruleScore;
        switch (ruleResult) {
            case "high_risk": ruleScore = 1.0; break;
            case "medium_risk": ruleScore = 0.5; break;
            default: ruleScore = 0.0;
        }

        double velocityScore;
        switch (velocityResult) {
            case "suspicious": velocityScore = 1.0; break;
            case "elevated": velocityScore = 0.5; break;
            default: velocityScore = 0.0;
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

        TaskResult result = new TaskResult(task);
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
