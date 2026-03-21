package sensordataprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Analyzes aggregated sensor metrics for patterns and anomalies.
 * Input: batchId, aggregatedMetrics
 * Output: trend, anomalies, forecastNext1h
 */
public class AnalyzePatternsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sen_analyze_patterns";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> metrics = (Map<String, Object>) task.getInputData().get("aggregatedMetrics");
        if (metrics == null) {
            metrics = Map.of();
        }

        double maxTemp = 0;
        Object maxTempObj = metrics.get("maxTemperature");
        if (maxTempObj instanceof Number) {
            maxTemp = ((Number) maxTempObj).doubleValue();
        } else if (maxTempObj instanceof String) {
            try { maxTemp = Double.parseDouble((String) maxTempObj); } catch (NumberFormatException ignored) {}
        }

        boolean hasAnomaly = maxTemp > 85;

        System.out.println("  [analyze] Max temp: " + maxTemp + "F, Anomaly: " + hasAnomaly);

        List<Map<String, Object>> anomalies = hasAnomaly
                ? List.of(Map.of("type", "high_temperature", "value", maxTemp, "threshold", 85))
                : List.of();

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("trend", "rising");
        result.getOutputData().put("anomalies", anomalies);
        result.getOutputData().put("forecastNext1h", 75.2);
        return result;
    }
}
