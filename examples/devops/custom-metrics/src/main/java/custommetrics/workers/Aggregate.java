package custommetrics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Aggregates raw data points over the specified window.
 */
public class Aggregate implements Worker {

    @Override
    public String getTaskDefName() {
        return "cus_aggregate";
    }

    @Override
    public TaskResult execute(Task task) {
        Object rawDataPoints = task.getInputData().get("rawDataPoints");
        String window = (String) task.getInputData().get("aggregationWindow");

        System.out.println("[cus_aggregate] Aggregating " + rawDataPoints + " data points (window: " + window + ")...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("metricCount", 4);
        result.getOutputData().put("aggregatedMetrics", List.of(
                Map.of("name", "order_processing_time", "p50", 1200, "p99", 5000),
                Map.of("name", "cart_abandonment_rate", "value", 12.5),
                Map.of("name", "checkout_success_count", "total", 8420),
                Map.of("name", "payment_retry_rate", "value", 2.3)
        ));
        return result;
    }
}
