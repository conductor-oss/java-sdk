package maintenancewindow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SuppressAlertsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mw_suppress_alerts";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [suppress] Alert suppression enabled for maintenance window");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("suppress_alerts", true);
        result.addOutputData("processed", true);
        return result;
    }
}
