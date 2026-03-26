package eventmonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Analyzes latency from raw metrics.
 * Input: metrics (rawMetrics map)
 * Output: latency (avgMs, p95Ms, p99Ms)
 */
public class AnalyzeLatencyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "em_analyze_latency";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> metrics = (Map<String, Object>) task.getInputData().get("metrics");

        int avgMs = 0;
        int p95Ms = 0;
        int p99Ms = 0;
        if (metrics != null) {
            if (metrics.get("avgLatencyMs") != null) {
                avgMs = ((Number) metrics.get("avgLatencyMs")).intValue();
            }
            if (metrics.get("p95LatencyMs") != null) {
                p95Ms = ((Number) metrics.get("p95LatencyMs")).intValue();
            }
            if (metrics.get("p99LatencyMs") != null) {
                p99Ms = ((Number) metrics.get("p99LatencyMs")).intValue();
            }
        }

        System.out.println("  [em_analyze_latency] Latency avg=" + avgMs + "ms, p95=" + p95Ms + "ms, p99=" + p99Ms + "ms");

        Map<String, Object> latency = Map.of(
                "avgMs", avgMs,
                "p95Ms", p95Ms,
                "p99Ms", p99Ms
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("latency", latency);
        return result;
    }
}
