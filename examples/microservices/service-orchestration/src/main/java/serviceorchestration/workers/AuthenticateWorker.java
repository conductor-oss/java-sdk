package serviceorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Authenticates a user and returns a JWT token.
 * Input: userId
 * Output: token, authenticated, userId
 */
public class AuthenticateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "so_authenticate";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        if (userId == null) {
            userId = "unknown";
        }

        System.out.println("  [so_authenticate] Authenticating user " + userId + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("token", "jwt-token-abc123");
        result.getOutputData().put("authenticated", true);
        result.getOutputData().put("userId", userId);
        return result;
    }
}
