package commitanalysis.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class DetectPatternsWorker implements Worker {
    @Override public String getTaskDefName() { return "cma_detect_patterns"; }
    @Override public TaskResult execute(Task task) {
        List<Map<String, String>> patterns = List.of(
            Map.of("type", "hotspot", "detail", "auth module has frequent fixes"),
            Map.of("type", "contributor", "detail", "alice focuses on features and refactors"),
            Map.of("type", "coupling", "detail", "search and notifications often change together")
        );
        System.out.println("  [patterns] Detected " + patterns.size() + " patterns");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("patterns", patterns);
        return result;
    }
}
