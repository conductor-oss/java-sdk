package ratelimitermicroservice.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Rejects a request when the rate limit quota is exceeded.
 * Input: clientId, retryAfter
 * Output: rejected, retryAfter
 */
public class RejectRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rl_reject_request";
    }

    @Override
    public TaskResult execute(Task task) {
        String clientId = (String) task.getInputData().get("clientId");
        if (clientId == null) clientId = "unknown";

        System.out.println("  [rl_reject_request] Rate limit exceeded for " + clientId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rejected", true);
        result.getOutputData().put("retryAfter", 30);
        return result;
    }
}
