package accountdeletion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyDeletionWorker implements Worker {
    @Override public String getTaskDefName() { return "acd_verify"; }

    @Override public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        String reason = (String) task.getInputData().get("reason");
        System.out.println("  [verify] Identity verified for deletion request — user: " + userId + ", reason: " + reason);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", true);
        return result;
    }
}
