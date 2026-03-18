package eventdrivensaga.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Creates an order in the saga.
 * Input: orderId, amount
 * Output: orderId, amount, orderStatus ("created")
 */
public class CreateOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ds_create_order";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        if (orderId == null) {
            orderId = "UNKNOWN";
        }

        Object amount = task.getInputData().get("amount");
        if (amount == null) {
            amount = 0;
        }

        System.out.println("  [ds_create_order] Created order " + orderId + " for $" + amount);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orderId", orderId);
        result.getOutputData().put("amount", amount);
        result.getOutputData().put("orderStatus", "created");
        return result;
    }
}
