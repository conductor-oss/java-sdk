package eventmonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Analyzes error rate from raw metrics.
 * Input: metrics (rawMetrics map)
 * Output: errorRate (percentage, failed, total)
 */
public class AnalyzeErrorsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "em_analyze_errors";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> metrics = (Map<String, Object>) task.getInputData().get("metrics");

        int failed = 0;
        int total = 0;
        if (metrics != null) {
            if (metrics.get("failedEvents") != null) {
                failed = ((Number) metrics.get("failedEvents")).intValue();
            }
            if (metrics.get("totalEvents") != null) {
                total = ((Number) metrics.get("totalEvents")).intValue();
            }
        }

        String percentage = "0.00";
        if (total > 0) {
            percentage = String.format("%.2f", (failed * 100.0) / total);
        }

        System.out.println("  [em_analyze_errors] Error rate: " + percentage + "% (" + failed + "/" + total + ")");

        Map<String, Object> errorRate = Map.of(
                "percentage", percentage,
                "failed", failed,
                "total", total
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("errorRate", errorRate);
        return result;
    }
}
