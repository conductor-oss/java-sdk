package nestedsubworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Fulfills an order after payment is complete.
 * Returns fulfillment status.
 * Used by the root workflow (Level 1: nested_order).
 */
public class FulfillWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nest_fulfill";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");

        if (orderId == null || orderId.isBlank()) {
            orderId = "UNKNOWN";
        }

        System.out.println("  [nest_fulfill] Fulfilling order " + orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("fulfilled", true);
        return result;
    }
}
