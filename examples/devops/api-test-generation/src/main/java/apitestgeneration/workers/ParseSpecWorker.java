package apitestgeneration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class ParseSpecWorker implements Worker {
    @Override public String getTaskDefName() { return "atg_parse_spec"; }
    @Override public TaskResult execute(Task task) {
        String format = (String) task.getInputData().getOrDefault("format", "openapi3");
        List<Map<String, Object>> endpoints = List.of(
            Map.of("path", "/users", "method", "GET", "params", List.of()),
            Map.of("path", "/users", "method", "POST", "params", List.of("name", "email")),
            Map.of("path", "/users/{id}", "method", "GET", "params", List.of("id")),
            Map.of("path", "/orders", "method", "POST", "params", List.of("userId", "items"))
        );
        System.out.println("  [parse] Parsed " + format + " spec — " + endpoints.size() + " endpoints");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("endpoints", endpoints);
        result.getOutputData().put("endpointCount", endpoints.size());
        return result;
    }
}
