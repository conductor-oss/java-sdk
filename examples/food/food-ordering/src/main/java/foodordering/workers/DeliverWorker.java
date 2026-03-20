package foodordering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

public class DeliverWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fod_deliver";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        System.out.println("  [deliver] Delivering order " + orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("delivery", Map.of(
                "orderId", orderId != null ? orderId : "ORD-5001",
                "status", "DELIVERED",
                "eta", "25 min"
        ));
        return result;
    }
}
