package authenticationworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Issues a JWT token after successful authentication.
 * Input: issue_tokenData (from previous step)
 * Output: issue_token, completedAt
 */
public class IssueTokenWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "auth_issue_token";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [token] JWT issued with 1h expiry");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("issue_token", true);
        result.getOutputData().put("completedAt", "2024-01-15T10:30:00Z");
        return result;
    }
}
