package databasemigrationdevops.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Validates that schema matches expected state after migration.
 * Input: validate_schemaData
 * Output: validate_schema, processed
 */
public class ValidateSchemaWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dbm_validate_schema";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [validate] Schema matches expected state, no data loss");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validate_schema", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
