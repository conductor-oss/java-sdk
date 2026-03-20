package usageanalytics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AlertWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "uag_alert";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [alert] Anomaly alerts raised");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("alertCount", 1);
        result.getOutputData().put("notified", true);
        return result;
    }
}
