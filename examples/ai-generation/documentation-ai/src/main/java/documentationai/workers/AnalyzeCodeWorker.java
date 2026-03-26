package documentationai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class AnalyzeCodeWorker implements Worker {
    @Override public String getTaskDefName() { return "doc_analyze_code"; }
    @Override public TaskResult execute(Task task) {
        String repoPath = (String) task.getInputData().getOrDefault("repoPath", "unknown");
        List<Map<String, Object>> modules = List.of(
            Map.of("name", "auth", "functions", 8, "classes", 2),
            Map.of("name", "api", "functions", 15, "classes", 4),
            Map.of("name", "utils", "functions", 12, "classes", 1)
        );
        System.out.println("  [analyze] Found " + modules.size() + " modules in " + repoPath);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("modules", modules);
        result.getOutputData().put("moduleCount", modules.size());
        return result;
    }
}
