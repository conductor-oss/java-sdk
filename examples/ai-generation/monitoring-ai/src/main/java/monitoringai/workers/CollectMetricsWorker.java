package monitoringai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class CollectMetricsWorker implements Worker {
    @Override public String getTaskDefName() { return "mai_collect_metrics"; }
    @Override public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().getOrDefault("serviceName", "unknown");
        String timeWindow = (String) task.getInputData().getOrDefault("timeWindow", "1h");
        Map<String, Map<String, Object>> metrics = Map.of(
            "cpu", Map.of("avg", 72, "max", 95, "p99", 91),
            "memory", Map.of("avg", 68, "max", 82, "p99", 80),
            "latency", Map.of("avg", 120, "max", 850, "p99", 620),
            "errorRate", Map.of("avg", 0.02, "max", 0.15, "p99", 0.12)
        );
        System.out.println("  [collect] Collected 4 metric types for " + serviceName + " (" + timeWindow + ")");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("metrics", metrics);
        result.getOutputData().put("metricCount", 4);
        return result;
    }
}
