package requestaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Fetches user profile data.
 * Input: userId
 * Output: name, email, tier
 */
public class FetchUserWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agg_fetch_user";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        if (userId == null) userId = "unknown";

        System.out.println("  [user] Fetching profile for " + userId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("name", "Alice");
        result.getOutputData().put("email", "alice@co.com");
        result.getOutputData().put("tier", "premium");
        return result;
    }
}
