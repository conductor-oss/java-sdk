package commitanalysis.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "cma_report"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<?> patterns = (List<?>) task.getInputData().getOrDefault("patterns", List.of());
        System.out.println("  [report] Generated analysis report with " + patterns.size() + " patterns");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("report", Map.of("patternCount", patterns.size(), "health", "good", "risk", "low"));
        return result;
    }
}
