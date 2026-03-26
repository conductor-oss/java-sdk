package deliverytracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class ConfirmWorker implements Worker {
    @Override public String getTaskDefName() { return "dlt_confirm"; }

    @Override public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        System.out.println("  [confirm] Delivery confirmed for order " + orderId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("delivery", Map.of("orderId", orderId != null ? orderId : "ORD-733", "status", "DELIVERED", "rating", 5));
        return result;
    }
}
