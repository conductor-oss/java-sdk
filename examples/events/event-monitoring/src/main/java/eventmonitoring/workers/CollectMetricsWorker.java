package eventmonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Collects raw metrics for a given pipeline and time range.
 * Input: pipelineName, timeRange
 * Output: rawMetrics (totalEvents, processedEvents, failedEvents, avgLatencyMs, p95LatencyMs, p99LatencyMs)
 */
public class CollectMetricsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "em_collect_metrics";
    }

    @Override
    public TaskResult execute(Task task) {
        String pipelineName = (String) task.getInputData().get("pipelineName");
        if (pipelineName == null) {
            pipelineName = "unknown-pipeline";
        }

        String timeRange = (String) task.getInputData().get("timeRange");
        if (timeRange == null) {
            timeRange = "last_1h";
        }

        System.out.println("  [em_collect_metrics] Collecting metrics for pipeline: " + pipelineName + ", range: " + timeRange);

        Map<String, Object> rawMetrics = Map.of(
                "totalEvents", 15420,
                "processedEvents", 15100,
                "failedEvents", 320,
                "avgLatencyMs", 45,
                "p95LatencyMs", 120,
                "p99LatencyMs", 500
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rawMetrics", rawMetrics);
        return result;
    }
}
