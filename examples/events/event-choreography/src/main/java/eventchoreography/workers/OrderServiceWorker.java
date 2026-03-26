package eventchoreography.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Processes an incoming order and calculates the total amount.
 * Input: orderId, items, customerId
 * Output: orderId, items, totalAmount (79.98), status ("created")
 */
public class OrderServiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ch_order_service";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        if (orderId == null) {
            orderId = "ORD-UNKNOWN";
        }

        Object items = task.getInputData().get("items");
        if (items == null) {
            items = List.of();
        }

        System.out.println("  [ch_order_service] Order " + orderId + ": processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orderId", orderId);
        result.getOutputData().put("items", items);
        result.getOutputData().put("totalAmount", 79.98);
        result.getOutputData().put("status", "created");
        return result;
    }
}
