package custommetrics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Registers custom metric definitions.
 */
public class DefineMetrics implements Worker {

    @Override
    public String getTaskDefName() {
        return "cus_define_metrics";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("[cus_define_metrics] Registering custom metric definitions...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("registeredMetrics", 4);
        result.getOutputData().put("metrics", List.of(
                Map.of("name", "order_processing_time", "type", "histogram"),
                Map.of("name", "cart_abandonment_rate", "type", "gauge"),
                Map.of("name", "checkout_success_count", "type", "counter"),
                Map.of("name", "payment_retry_rate", "type", "gauge")
        ));
        return result;
    }
}
