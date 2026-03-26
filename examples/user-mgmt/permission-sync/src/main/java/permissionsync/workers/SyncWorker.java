package permissionsync.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class SyncWorker implements Worker {
    @Override public String getTaskDefName() { return "pms_sync"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [sync] Applied 6 permission changes to target systems");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("syncedCount", 6);
        result.getOutputData().put("syncResults", Map.of("success", 6, "failed", 0));
        return result;
    }
}
