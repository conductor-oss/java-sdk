package bugtriage.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ParseReportWorker implements Worker {
    @Override public String getTaskDefName() { return "btg_parse_report"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        Map<String, Object> report = (Map<String, Object>) task.getInputData().getOrDefault("bugReport", Map.of());
        String title = (String) report.getOrDefault("title", "Unknown");
        String description = (String) report.getOrDefault("description", "");
        String component = (String) report.getOrDefault("component", "general");
        System.out.println("  [parse] Parsed bug report: \"" + title + "\"");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bugId", "BUG-2024-642");
        result.getOutputData().put("title", title);
        result.getOutputData().put("description", description);
        result.getOutputData().put("component", component);
        return result;
    }
}
