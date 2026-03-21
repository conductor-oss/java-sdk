package sessionmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class RevokeSessionWorker implements Worker {
    @Override public String getTaskDefName() { return "ses_revoke"; }

    @Override public TaskResult execute(Task task) {
        String sessionId = (String) task.getInputData().get("sessionId");
        System.out.println("  [revoke] Session " + sessionId + " revoked");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("revoked", true);
        result.getOutputData().put("revokedAt", Instant.now().toString());
        return result;
    }
}
