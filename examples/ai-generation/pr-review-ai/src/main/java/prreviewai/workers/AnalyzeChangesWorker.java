package prreviewai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class AnalyzeChangesWorker implements Worker {
    @Override public String getTaskDefName() { return "prr_analyze_changes"; }
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> issues = List.of(
            Map.of("file", "src/auth.js", "line", 42, "type", "security", "message", "Potential SQL injection"),
            Map.of("file", "src/api.js", "line", 15, "type", "style", "message", "Missing error handling")
        );
        Map<String, Object> analysis = Map.of("issues", issues, "complexity", "medium");
        System.out.println("  [analyze] Found " + issues.size() + " issues, complexity: medium");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("analysis", analysis);
        result.getOutputData().put("issueCount", issues.size());
        return result;
    }
}
