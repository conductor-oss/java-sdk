package claimcheck.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Processes the retrieved payload and computes metric averages.
 * Input: retrievedPayload
 * Output: processed, metricsAnalyzed, summary
 */
public class ProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "clc_process";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object payloadObj = task.getInputData().get("retrievedPayload");
        Map<String, Object> payload = (payloadObj instanceof Map)
                ? (Map<String, Object>) payloadObj
                : Map.of();

        Object dataObj = payload.get("data");
        List<Map<String, Object>> metrics = (dataObj instanceof List)
                ? (List<Map<String, Object>>) dataObj
                : List.of();

        System.out.println("  [process] Processing retrieved payload — " + metrics.size() + " metrics");

        List<Map<String, Object>> summary = new ArrayList<>();
        for (Map<String, Object> m : metrics) {
            String metric = (String) m.getOrDefault("metric", "unknown");
            List<Number> values = (m.get("values") instanceof List)
                    ? (List<Number>) m.get("values")
                    : List.of();
            double avg = values.isEmpty() ? 0.0
                    : values.stream().mapToDouble(Number::doubleValue).sum() / values.size();
            summary.add(Map.of("metric", metric, "avg", avg));
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("metricsAnalyzed", metrics.size());
        result.getOutputData().put("summary", summary);
        return result;
    }
}
