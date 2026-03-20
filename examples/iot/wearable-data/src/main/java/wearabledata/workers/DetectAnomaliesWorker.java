package wearabledata.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DetectAnomaliesWorker implements Worker {
    @Override public String getTaskDefName() { return "wer_detect_anomalies"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [anomaly] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("anomalyCount", "anomalies.length");
        return r;
    }
}
