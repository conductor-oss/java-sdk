package creditscoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Computes a weighted credit score from factor data.
 * Input: applicantId, factors
 * Output: score, model, computedAt
 *
 * Score formula: 300 + (weightedAvg / 100) * 550
 * where weightedAvg = sum(factor.score * factor.weight) / 100
 */
public class ScoreWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "csc_score";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> factors = (Map<String, Object>) task.getInputData().get("factors");
        if (factors == null) factors = Map.of();

        double weightedScore = 0;
        for (Object value : factors.values()) {
            if (value instanceof Map) {
                Map<String, Object> f = (Map<String, Object>) value;
                int score = toInt(f.get("score"));
                int weight = toInt(f.get("weight"));
                weightedScore += (score * weight) / 100.0;
            }
        }
        int finalScore = (int) Math.round(300 + (weightedScore / 100.0) * 550);

        System.out.println("  [score] Computed credit score: " + finalScore);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("score", finalScore);
        result.getOutputData().put("model", "FICO-8");
        result.getOutputData().put("computedAt", "2026-01-15T10:00:00Z");
        return result;
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
