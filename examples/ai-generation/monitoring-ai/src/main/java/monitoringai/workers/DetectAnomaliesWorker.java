package monitoringai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class DetectAnomaliesWorker implements Worker {
    @Override public String getTaskDefName() { return "mai_detect_anomalies"; }
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> anomalies = List.of(
            Map.of("metric", "latency", "severity", "high", "value", 850, "threshold", 500),
            Map.of("metric", "cpu", "severity", "medium", "value", 95, "threshold", 85)
        );
        System.out.println("  [detect] Found " + anomalies.size() + " anomalies");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("anomalies", anomalies);
        result.getOutputData().put("anomalyCount", anomalies.size());
        return result;
    }
}
