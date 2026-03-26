package observabilitypipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DetectAnomaliesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "op_detect_anomalies";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [anomaly] Detected 2 anomalies: latency spike, error rate increase");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("detect_anomalies", true);
        result.addOutputData("processed", true);
        return result;
    }
}
