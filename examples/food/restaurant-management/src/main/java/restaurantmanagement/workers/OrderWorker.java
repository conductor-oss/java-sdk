package restaurantmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class OrderWorker implements Worker {
    @Override public String getTaskDefName() { return "rst_order"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [order] Taking order at table " + task.getInputData().get("tableId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("orderId", "ORD-732");
        result.addOutputData("items", List.of("Steak", "Pasta", "Wine"));
        result.addOutputData("total", 85.50);
        return result;
    }
}
