package salesforceintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Syncs records to a CRM target.
 * Input: updatedCount, syncTarget
 * Output: synced, syncedAt
 */
public class SyncCrmWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sfc_sync_crm";
    }

    @Override
    public TaskResult execute(Task task) {
        Object updatedCount = task.getInputData().get("updatedCount");
        String syncTarget = (String) task.getInputData().get("syncTarget");
        System.out.println("  [sync] Synced " + updatedCount + " records to " + syncTarget);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("synced", true);
        result.getOutputData().put("syncedAt", "" + java.time.Instant.now().toString());
        return result;
    }
}
