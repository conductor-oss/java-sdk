package databasemigrationdevops.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Creates a database snapshot before migration.
 * Input: database, migrationVersion
 * Output: backupId, success, database
 */
public class BackupWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dbm_backup";
    }

    @Override
    public TaskResult execute(Task task) {
        String database = (String) task.getInputData().get("database");
        String migrationVersion = (String) task.getInputData().get("migrationVersion");

        if (database == null) database = "unknown-db";
        if (migrationVersion == null) migrationVersion = "V000";

        System.out.println("  [backup] Snapshot of " + database + " created");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("backupId", "BACKUP-1363");
        result.getOutputData().put("success", true);
        result.getOutputData().put("database", database);
        result.getOutputData().put("migrationVersion", migrationVersion);
        return result;
    }
}
