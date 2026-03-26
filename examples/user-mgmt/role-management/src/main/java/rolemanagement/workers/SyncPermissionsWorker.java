package rolemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class SyncPermissionsWorker implements Worker {
    @Override public String getTaskDefName() { return "rom_sync_permissions"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [sync] Permissions synced to IAM, API gateway, and database");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("synced", true);
        result.getOutputData().put("targets", List.of("iam", "api-gateway", "database"));
        return result;
    }
}
