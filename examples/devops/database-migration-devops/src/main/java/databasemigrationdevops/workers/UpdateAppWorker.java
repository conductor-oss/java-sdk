package databasemigrationdevops.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Deploys the application with updated schema mapping.
 * Input: update_appData
 * Output: update_app, completedAt
 */
public class UpdateAppWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dbm_update_app";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [app] Application deployed with updated schema mapping");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("update_app", true);
        result.getOutputData().put("completedAt", "2026-03-14T00:00:00Z");
        return result;
    }
}
