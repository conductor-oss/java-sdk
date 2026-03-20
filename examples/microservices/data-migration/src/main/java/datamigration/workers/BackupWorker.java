package datamigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BackupWorker implements Worker {
    @Override public String getTaskDefName() { return "dm_backup"; }
    @Override public TaskResult execute(Task task) {
        String src = (String) task.getInputData().getOrDefault("sourceDb", "unknown");
        System.out.println("  [backup] Backing up " + src);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("backupId", "BKP-" + System.currentTimeMillis());
        r.getOutputData().put("sizeGb", 12);
        return r;
    }
}
