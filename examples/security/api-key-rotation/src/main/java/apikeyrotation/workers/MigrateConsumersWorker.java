package apikeyrotation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MigrateConsumersWorker implements Worker {

    @Override public String getTaskDefName() { return "akr_migrate_consumers"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [migrate] 5 consumers updated to new key");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("migrate_consumers", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
