package kitchenworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class ServeWorker implements Worker {
    @Override public String getTaskDefName() { return "kit_serve"; }
    @Override public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        String tableId = (String) task.getInputData().get("tableId");
        System.out.println("  [serve] Serving order " + orderId + " to table " + tableId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("service", Map.of("orderId", orderId != null ? orderId : "ORD-735",
                "tableId", tableId != null ? tableId : "T-5", "status", "SERVED", "totalTime", "24 min"));
        return result;
    }
}
