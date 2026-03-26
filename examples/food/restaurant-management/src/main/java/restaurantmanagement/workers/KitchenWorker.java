package restaurantmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class KitchenWorker implements Worker {
    @Override public String getTaskDefName() { return "rst_kitchen"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [kitchen] Preparing items for order " + task.getInputData().get("orderId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("prepared", true);
        result.addOutputData("cookTime", "22 min");
        return result;
    }
}
