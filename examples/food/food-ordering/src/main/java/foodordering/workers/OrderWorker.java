package foodordering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class OrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fod_order";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerId = (String) task.getInputData().get("customerId");
        System.out.println("  [order] Placing order for customer " + customerId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("orderId", "ORD-5001");
        result.addOutputData("total", 24.98);
        result.addOutputData("items", 2);
        return result;
    }
}
