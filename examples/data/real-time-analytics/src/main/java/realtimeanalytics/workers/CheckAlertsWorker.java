package realtimeanalytics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks alert rules against current aggregates and triggers alerts.
 * Input: aggregates (map), alertRules (list)
 * Output: alerts (list), alertCount
 */
public class CheckAlertsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ry_check_alerts";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> agg = (Map<String, Object>) task.getInputData().getOrDefault("aggregates", Map.of());

        Map<String, Object> currentWindow = (Map<String, Object>) agg.getOrDefault("currentWindow", Map.of());
        List<Map<String, String>> alerts = new ArrayList<>();

        int anomalyCount = toInt(currentWindow.get("anomalyCount"));
        if (anomalyCount > 1) {
            Map<String, String> alert = new LinkedHashMap<>();
            alert.put("rule", "anomaly_threshold");
            alert.put("severity", "warning");
            alert.put("message", anomalyCount + " anomalies detected in current window");
            alerts.add(alert);
        }

        int errorCount = toInt(currentWindow.get("errorCount"));
        if (errorCount > 0) {
            Map<String, String> alert = new LinkedHashMap<>();
            alert.put("rule", "error_detected");
            alert.put("severity", "critical");
            alert.put("message", errorCount + " errors in current window");
            alerts.add(alert);
        }

        String p99Str = (String) agg.getOrDefault("p99Latency", "0ms");
        int p99 = Integer.parseInt(p99Str.replace("ms", ""));
        if (p99 > 500) {
            Map<String, String> alert = new LinkedHashMap<>();
            alert.put("rule", "high_latency");
            alert.put("severity", "warning");
            alert.put("message", "P99 latency " + p99Str + " exceeds 500ms threshold");
            alerts.add(alert);
        }

        String alertSummary = alerts.isEmpty() ? "none"
                : alerts.stream().map(a -> a.get("severity") + ":" + a.get("rule"))
                .reduce((a, b) -> a + ", " + b).orElse("none");
        System.out.println("  [alerts] " + alerts.size() + " alerts triggered: " + alertSummary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("alerts", alerts);
        result.getOutputData().put("alertCount", alerts.size());
        return result;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        return 0;
    }
}
