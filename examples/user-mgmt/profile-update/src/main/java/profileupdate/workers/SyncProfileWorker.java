package profileupdate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Syncs profile changes to downstream services.
 * Input: userId, updatedFields
 * Output: synced, services
 */
public class SyncProfileWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pfu_sync";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [sync] Profile synced to 3 downstream services");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("synced", true);
        result.getOutputData().put("services", List.of("crm", "analytics", "email"));
        return result;
    }
}
