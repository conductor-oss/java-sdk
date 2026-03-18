package bulkheadpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Executes the request within the allocated pool.
 * Input: request, poolId
 * Output: response
 */
public class ExecuteRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bh_execute_request";
    }

    @Override
    public TaskResult execute(Task task) {
        Object request = task.getInputData().get("request");
        String poolId = (String) task.getInputData().get("poolId");
        if (poolId == null) poolId = "unknown";

        System.out.println("  [bh_execute_request] Processing request in pool " + poolId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("response", Map.of("status", "ok"));
        return result;
    }
}
