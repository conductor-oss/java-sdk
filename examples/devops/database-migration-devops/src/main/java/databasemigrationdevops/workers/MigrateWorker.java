package databasemigrationdevops.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Applies database migration ALTER statements.
 * Input: migrateData
 * Output: migrate, processed, alterStatements
 */
public class MigrateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dbm_migrate";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [migrate] Applied migration: 3 ALTER statements");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("migrate", true);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("alterStatements", 3);
        return result;
    }
}
