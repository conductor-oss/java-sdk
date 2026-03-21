package toolratelimit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks the rate limit for the given tool/API key combination.
 * Demonstrates near-limit conditions: quotaUsed=98, quotaLimit=100, quotaRemaining=2,
 * decision="throttled" (since remaining <= 2).
 */
public class CheckRateLimitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rl_check_rate_limit";
    }

    @Override
    public TaskResult execute(Task task) {
        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown_tool";
        }

        String apiKey = (String) task.getInputData().get("apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = "unknown_key";
        }

        System.out.println("  [rl_check_rate_limit] Checking rate limit for tool: " + toolName);

        int quotaUsed = 98;
        int quotaLimit = 100;
        int quotaRemaining = quotaLimit - quotaUsed;
        String decision = quotaRemaining <= 2 ? "throttled" : "allowed";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("quotaUsed", quotaUsed);
        result.getOutputData().put("quotaLimit", quotaLimit);
        result.getOutputData().put("quotaRemaining", quotaRemaining);
        result.getOutputData().put("decision", decision);
        result.getOutputData().put("retryAfterMs", 5000);
        result.getOutputData().put("queuePosition", 3);
        result.getOutputData().put("windowResetAt", "2026-03-08T10:01:00Z");
        return result;
    }
}
