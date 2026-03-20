package ratelimitermicroservice.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks the rate limit quota for a client on a given endpoint.
 * Input: clientId, endpoint
 * Output: allowed, remaining, limit, window
 *
 * Deterministic: always reports 42/100 used, so allowed=true, remaining=58.
 */
public class CheckQuotaWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rl_check_quota";
    }

    @Override
    public TaskResult execute(Task task) {
        String clientId = (String) task.getInputData().get("clientId");
        String endpoint = (String) task.getInputData().get("endpoint");
        if (clientId == null) clientId = "unknown";
        if (endpoint == null) endpoint = "/unknown";

        System.out.println("  [rl_check_quota] " + clientId + ": 42/100 requests used on " + endpoint);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("allowed", true);
        result.getOutputData().put("remaining", 58);
        result.getOutputData().put("limit", 100);
        result.getOutputData().put("window", "1m");
        return result;
    }
}
