package accountdeletion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.UUID;

public class BackupWorker implements Worker {
    @Override public String getTaskDefName() { return "acd_backup"; }

    @Override public TaskResult execute(Task task) {
        String backupId = "BKP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.println("  [backup] User data backed up -> " + backupId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("backupId", backupId);
        result.getOutputData().put("sizeBytes", 15360640);
        result.getOutputData().put("retainDays", 30);
        return result;
    }
}
