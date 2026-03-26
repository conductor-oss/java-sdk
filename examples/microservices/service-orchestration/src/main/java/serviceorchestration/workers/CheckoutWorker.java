package serviceorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes checkout for a cart.
 * Input: userId, cartId, total
 * Output: orderId, total, orderStatus, estimatedDelivery
 */
public class CheckoutWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "so_checkout";
    }

    @Override
    public TaskResult execute(Task task) {
        String cartId = (String) task.getInputData().get("cartId");
        Object totalObj = task.getInputData().get("total");
        double total = 0.0;
        if (totalObj instanceof Number) {
            total = ((Number) totalObj).doubleValue();
        }

        System.out.println("  [so_checkout] Processing checkout for cart " + cartId + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orderId", "ORD-20240301-001");
        result.getOutputData().put("total", total);
        result.getOutputData().put("orderStatus", "confirmed");
        result.getOutputData().put("estimatedDelivery", "2024-03-05");
        return result;
    }
}
