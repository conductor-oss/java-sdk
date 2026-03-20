package maintenancewindow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RestoreNormalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mw_restore_normal";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [restore] Alerts re-enabled, status page updated, maintenance ended");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("restore_normal", true);
        return result;
    }
}
