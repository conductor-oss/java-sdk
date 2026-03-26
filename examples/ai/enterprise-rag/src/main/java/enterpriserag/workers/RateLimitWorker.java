package enterpriserag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker that checks rate limits for a user.
 * Takes userId, returns allowed status and rate limit details.
 */
public class RateLimitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "er_rate_limit";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");

        System.out.println("  [rate_limit] Checking rate limit for user=" + userId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("allowed", true);
        result.getOutputData().put("current", 12);
        result.getOutputData().put("limit", 60);
        result.getOutputData().put("remaining", 48);
        return result;
    }
}
