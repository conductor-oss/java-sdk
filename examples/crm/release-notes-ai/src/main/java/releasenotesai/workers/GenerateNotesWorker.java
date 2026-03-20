package releasenotesai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class GenerateNotesWorker implements Worker {
    @Override public String getTaskDefName() { return "rna_generate_notes"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        Map<String, List<String>> cats = (Map<String, List<String>>) task.getInputData().getOrDefault("categories", Map.of());
        long sections = cats.values().stream().filter(v -> !v.isEmpty()).count();
        System.out.println("  [generate] Generated release notes with " + sections + " sections");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("releaseNotes", cats);
        result.getOutputData().put("sectionCount", sections);
        return result;
    }
}
