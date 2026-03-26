package securityorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class IngestAlertWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "soar_ingest_alert";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [ingest] Alert ALERT-2024-commission-insurance from crowdstrike");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("ingest_alertId", "INGEST_ALERT-1352");
        result.addOutputData("success", true);
        return result;
    }
}
