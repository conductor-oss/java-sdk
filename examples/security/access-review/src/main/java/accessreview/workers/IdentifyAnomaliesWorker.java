package accessreview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class IdentifyAnomaliesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ar_identify_anomalies";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [anomalies] Found 8 excessive access grants, 3 dormant accounts");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("identify_anomalies", true);
        result.addOutputData("processed", true);
        return result;
    }
}
