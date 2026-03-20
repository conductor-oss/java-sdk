package observabilitypipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AlertOrStoreWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "op_alert_or_store";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [alert] Alerts sent for 2 anomalies, metrics stored in Grafana");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("alert_or_store", true);
        return result;
    }
}
