package incidentai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class DetectWorker implements Worker {
    @Override public String getTaskDefName() { return "iai_detect"; }
    @Override public TaskResult execute(Task task) {
        String alertId = (String) task.getInputData().getOrDefault("alertId", "unknown");
        String serviceName = (String) task.getInputData().getOrDefault("serviceName", "unknown");
        System.out.println("  [detect] Alert " + alertId + " for " + serviceName);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("incidentId", "INC-2024-650");
        result.getOutputData().put("incidentDetails", Map.of(
            "type", "high_error_rate",
            "errorRate", 0.15,
            "affectedEndpoints", List.of("/api/checkout", "/api/payment")
        ));
        return result;
    }
}
