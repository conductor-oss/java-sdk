package multifactorauth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.UUID;

public class GrantAccessWorker implements Worker {
    @Override public String getTaskDefName() { return "mfa_grant"; }

    @Override public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        System.out.println("  [grant] Access granted to " + userId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("accessGranted", true);
        result.getOutputData().put("token", "mfa_tok_" + UUID.randomUUID().toString().substring(0, 12));
        result.getOutputData().put("expiresIn", 5500);
        return result;
    }
}
