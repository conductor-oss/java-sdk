package maintenancewindow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ExecuteMaintenanceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mw_execute_maintenance";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [execute] version-upgrade completed successfully");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("execute_maintenance", true);
        result.addOutputData("processed", true);
        return result;
    }
}
