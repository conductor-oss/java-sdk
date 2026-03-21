package testgeneration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class AnalyzeCodeWorker implements Worker {
    @Override public String getTaskDefName() { return "tge_analyze_code"; }
    @Override public TaskResult execute(Task task) {
        String file = (String) task.getInputData().getOrDefault("sourceFile", "app.js");
        List<Map<String, Object>> functions = List.of(
            Map.of("name", "calculateTotal", "params", List.of("items", "tax"), "lines", 12),
            Map.of("name", "validateEmail", "params", List.of("email"), "lines", 8),
            Map.of("name", "formatCurrency", "params", List.of("amount", "locale"), "lines", 5)
        );
        System.out.println("  [analyze] Parsed " + file + " — found " + functions.size() + " functions");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("functions", functions);
        result.getOutputData().put("functionCount", functions.size());
        return result;
    }
}
