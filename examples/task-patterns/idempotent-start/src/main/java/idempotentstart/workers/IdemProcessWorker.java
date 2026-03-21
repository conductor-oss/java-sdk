package idempotentstart.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Idempotent process worker.
 * Takes an orderId and amount, returns a deterministic result based on orderId.
 *
 * This worker is used in the idempotent_demo workflow to demonstrate
 * correlationId-based dedup and search-based idempotency.
 */
public class IdemProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "idem_process";
    }

    @Override
    public TaskResult execute(Task task) {
        Object orderIdRaw = task.getInputData().get("orderId");
        String orderId;
        if (orderIdRaw == null) {
            orderId = "unknown";
        } else {
            orderId = String.valueOf(orderIdRaw);
            if (orderId.isBlank()) {
                orderId = "unknown";
            }
        }

        System.out.println("  [idem_process] Processing order: " + orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orderId", orderId);
        result.getOutputData().put("result", "order-" + orderId + "-done");
        return result;
    }
}
