package summarizationpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class ExtractSectionsWorker implements Worker {
    @Override public String getTaskDefName() { return "sum_extract_sections"; }
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> sections = List.of(
            Map.of("title", "Introduction", "wordCount", 450, "key", "Machine learning advances"),
            Map.of("title", "Methodology", "wordCount", 820, "key", "Transformer architecture with attention"),
            Map.of("title", "Results", "wordCount", 600, "key", "95.2% accuracy on benchmark"),
            Map.of("title", "Conclusion", "wordCount", 280, "key", "Significant improvement over baselines"));
        System.out.println("  [extract] Extracted " + sections.size() + " sections (2150 words)");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("sections", sections); r.getOutputData().put("sectionCount", sections.size()); r.getOutputData().put("totalWords", 2150);
        return r;
    }
}
