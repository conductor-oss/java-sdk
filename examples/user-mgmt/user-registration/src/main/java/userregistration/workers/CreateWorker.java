package userregistration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Creates the user record.
 * Input: username, email, valid
 * Output: userId, username, createdAt
 */
public class CreateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ur_create";
    }

    @Override
    public TaskResult execute(Task task) {
        String username = (String) task.getInputData().get("username");
        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        System.out.println("  [create] User record created -> " + userId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("userId", userId);
        result.getOutputData().put("username", username);
        result.getOutputData().put("createdAt", Instant.now().toString());
        return result;
    }
}
