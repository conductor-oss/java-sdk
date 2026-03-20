package sessionmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.UUID;

public class CreateSessionWorker implements Worker {
    @Override public String getTaskDefName() { return "ses_create"; }

    @Override public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        String sessionId = "SES-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        System.out.println("  [create] Session created -> " + sessionId + " for " + userId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sessionId", sessionId);
        result.getOutputData().put("token", "jwt_" + UUID.randomUUID().toString().substring(0, 16));
        result.getOutputData().put("expiresIn", 3600);
        return result;
    }
}
