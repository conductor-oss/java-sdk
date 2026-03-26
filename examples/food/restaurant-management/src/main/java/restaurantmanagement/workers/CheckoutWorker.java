package restaurantmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class CheckoutWorker implements Worker {
    @Override public String getTaskDefName() { return "rst_checkout"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [checkout] Generating bill for table " + task.getInputData().get("tableId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("bill", Map.of("orderId", task.getInputData().getOrDefault("orderId", "ORD-732"), "subtotal", 85.50, "tax", 7.27, "total", 92.77));
        return result;
    }
}
