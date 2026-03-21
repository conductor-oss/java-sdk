package ragevaluation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Worker that aggregates evaluation scores from faithfulness, relevance, and coherence.
 * Computes an overall average score, determines a verdict (PASS/MARGINAL/FAIL),
 * and returns a breakdown of all metrics.
 */
public class AggregateScoresWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "re_aggregate_scores";
    }

    @Override
    public TaskResult execute(Task task) {
        double faithfulnessScore = toDouble(task.getInputData().get("faithfulnessScore"));
        String faithfulnessReason = (String) task.getInputData().get("faithfulnessReason");
        double relevanceScore = toDouble(task.getInputData().get("relevanceScore"));
        String relevanceReason = (String) task.getInputData().get("relevanceReason");
        double coherenceScore = toDouble(task.getInputData().get("coherenceScore"));
        String coherenceReason = (String) task.getInputData().get("coherenceReason");

        double overallScore = (faithfulnessScore + relevanceScore + coherenceScore) / 3.0;
        // Round to 4 decimal places
        overallScore = Math.round(overallScore * 10000.0) / 10000.0;

        String verdict;
        if (overallScore >= 0.8) {
            verdict = "PASS";
        } else if (overallScore >= 0.6) {
            verdict = "MARGINAL";
        } else {
            verdict = "FAIL";
        }

        Map<String, Object> breakdown = new LinkedHashMap<>();

        Map<String, Object> faithfulness = new LinkedHashMap<>();
        faithfulness.put("score", faithfulnessScore);
        faithfulness.put("reason", faithfulnessReason);
        breakdown.put("faithfulness", faithfulness);

        Map<String, Object> relevance = new LinkedHashMap<>();
        relevance.put("score", relevanceScore);
        relevance.put("reason", relevanceReason);
        breakdown.put("relevance", relevance);

        Map<String, Object> coherence = new LinkedHashMap<>();
        coherence.put("score", coherenceScore);
        coherence.put("reason", coherenceReason);
        breakdown.put("coherence", coherence);

        System.out.println("  [aggregate] overallScore=" + overallScore
                + ", verdict=" + verdict
                + " (faithfulness=" + faithfulnessScore
                + ", relevance=" + relevanceScore
                + ", coherence=" + coherenceScore + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("overallScore", overallScore);
        result.getOutputData().put("verdict", verdict);
        result.getOutputData().put("breakdown", breakdown);
        return result;
    }

    /**
     * Safely converts a value to double, handling both Number and String types.
     */
    static double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }
}
