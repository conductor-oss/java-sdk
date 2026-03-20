package datamigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MigrateWorker implements Worker {
    @Override public String getTaskDefName() { return "dm_migrate"; }
    @Override public TaskResult execute(Task task) {
        String target = (String) task.getInputData().getOrDefault("targetDb", "unknown");
        System.out.println("  [migrate] Migrating 50,000 records to " + target);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("recordCount", 50000);
        r.getOutputData().put("durationSec", 120);
        return r;
    }
}
