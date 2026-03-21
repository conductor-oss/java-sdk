package logprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates metrics from parsed log entries.
 * Input: entries, patterns
 * Output: metrics
 */
public class AggregateMetricsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lp_aggregate_metrics";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> entries = (List<Map<String, Object>>) task.getInputData().get("entries");
        if (entries == null) {
            entries = List.of();
        }

        Map<String, Map<String, Object>> byService = new LinkedHashMap<>();
        long errorCount = 0;
        for (Map<String, Object> e : entries) {
            String service = (String) e.getOrDefault("service", "unknown");
            byService.computeIfAbsent(service, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("total", 0);
                m.put("errors", 0);
                return m;
            });
            Map<String, Object> svc = byService.get(service);
            svc.put("total", (int) svc.get("total") + 1);
            if (Boolean.TRUE.equals(e.get("isError"))) {
                svc.put("errors", (int) svc.get("errors") + 1);
                errorCount++;
            }
        }

        String errorRate = entries.isEmpty() ? "0.0%"
                : String.format("%.1f%%", (errorCount * 100.0) / entries.size());

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalEntries", entries.size());
        metrics.put("errorRate", errorRate);
        metrics.put("byService", byService);

        System.out.println("  [metrics] Error rate: " + errorRate + ", services: " + String.join(", ", byService.keySet()));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("metrics", metrics);
        return result;
    }
}
