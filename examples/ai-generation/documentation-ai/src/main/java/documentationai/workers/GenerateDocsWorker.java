package documentationai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class GenerateDocsWorker implements Worker {
    @Override public String getTaskDefName() { return "doc_generate_docs"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> modules = (List<Map<String, Object>>) task.getInputData().getOrDefault("modules", List.of());
        int pages = modules.size() * 3;
        List<Map<String, Object>> rawDocs = new ArrayList<>();
        for (Map<String, Object> m : modules) {
            rawDocs.add(Map.of("module", m.get("name"), "sections", 3));
        }
        System.out.println("  [generate] Generated " + pages + " documentation pages");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rawDocs", rawDocs);
        result.getOutputData().put("pageCount", pages);
        return result;
    }
}
