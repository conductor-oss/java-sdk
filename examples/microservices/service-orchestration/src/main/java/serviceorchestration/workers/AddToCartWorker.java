package serviceorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Adds items to a shopping cart.
 * Input: userId, product, quantity
 * Output: cartId, items, total
 */
public class AddToCartWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "so_add_to_cart";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        Object quantityObj = task.getInputData().get("quantity");
        int quantity = 1;
        if (quantityObj instanceof Number) {
            quantity = ((Number) quantityObj).intValue();
        }

        System.out.println("  [so_add_to_cart] Adding " + quantity + "x to cart for user " + userId + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cartId", "CART-7891");
        result.getOutputData().put("items", 1);
        result.getOutputData().put("total", 79.99 * quantity);
        return result;
    }
}
