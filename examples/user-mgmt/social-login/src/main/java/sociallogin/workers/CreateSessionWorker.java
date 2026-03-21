package sociallogin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.UUID;

public class CreateSessionWorker implements Worker {
    @Override public String getTaskDefName() { return "slo_session"; }
    @Override public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        String token = "sess_" + UUID.randomUUID().toString().substring(0, 16);
        System.out.println("  [session] Session created for " + userId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sessionToken", token);
        result.getOutputData().put("expiresIn", 86400);
        return result;
    }
}
