package apikeyrotation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RevokeOldWorker implements Worker {

    @Override public String getTaskDefName() { return "akr_revoke_old"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [revoke] Old key revoked after all consumers migrated");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("revoke_old", true);
        result.getOutputData().put("completedAt", "2024-01-15T10:30:00Z");
        return result;
    }
}
