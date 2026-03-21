package maintenancewindow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NotifyStartWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mw_notify_start";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [notify] Maintenance window started for database-cluster (2h)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("notify_startId", "NOTIFY_START-1336");
        result.addOutputData("success", true);
        return result;
    }
}
