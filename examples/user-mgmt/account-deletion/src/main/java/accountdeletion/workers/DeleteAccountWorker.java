package accountdeletion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

public class DeleteAccountWorker implements Worker {
    @Override public String getTaskDefName() { return "acd_delete"; }

    @Override public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        System.out.println("  [delete] Account " + userId + " and all associated data deleted");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deleted", true);
        result.getOutputData().put("tablesCleared", 12);
        result.getOutputData().put("deletedAt", Instant.now().toString());
        return result;
    }
}
