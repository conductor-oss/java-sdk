package apigatewayrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks rate limits for a client.
 * Input: clientId, path
 * Output: allowed, remaining
 */
public class RateCheckWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gw_rate_check";
    }

    @Override
    public TaskResult execute(Task task) {
        String clientId = (String) task.getInputData().get("clientId");
        if (clientId == null) clientId = "unknown";

        System.out.println("  [rate] Client " + clientId + ": 45/100 requests used");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("allowed", true);
        result.getOutputData().put("remaining", 55);
        return result;
    }
}
