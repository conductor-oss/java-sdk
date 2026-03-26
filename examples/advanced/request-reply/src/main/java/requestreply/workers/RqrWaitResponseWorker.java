package requestreply.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RqrWaitResponseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rqr_wait_response";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [wait] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("correlationId", task.getInputData().getOrDefault("correlationId", ""));
        result.getOutputData().put("response", java.util.Map.of("statusCode", 200, "body", java.util.Map.of("available", true, "inventory", 42)));
        result.getOutputData().put("latencyMs", 85);
        return result;
    }
}