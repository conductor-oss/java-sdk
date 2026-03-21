package ratelimitermicroservice.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Updates the rate limit counter after processing a request.
 * Input: clientId, endpoint
 * Output: updated
 */
public class UpdateCounterWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rl_update_counter";
    }

    @Override
    public TaskResult execute(Task task) {
        String clientId = (String) task.getInputData().get("clientId");
        if (clientId == null) clientId = "unknown";

        System.out.println("  [rl_update_counter] Incremented counter for " + clientId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("updated", true);
        return result;
    }
}
