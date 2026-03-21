package incidentai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class DiagnoseWorker implements Worker {
    @Override public String getTaskDefName() { return "iai_diagnose"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        Map<String, Object> details = (Map<String, Object>) task.getInputData().getOrDefault("incidentDetails", Map.of());
        String type = (String) details.getOrDefault("type", "unknown");
        List<?> endpoints = (List<?>) details.getOrDefault("affectedEndpoints", List.of());
        String diagnosis = type + ": payment gateway timeout causing 5xx on " + endpoints.size() + " endpoints";
        System.out.println("  [diagnose] " + diagnosis);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("diagnosis", diagnosis);
        return result;
    }
}
