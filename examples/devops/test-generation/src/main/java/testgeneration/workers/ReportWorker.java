package testgeneration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "tge_report"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> valid = (List<Map<String, Object>>) task.getInputData().getOrDefault("validatedTests", List.of());
        int total = 0;
        for (Map<String, Object> t : valid) {
            total += ((List<?>) t.get("testCases")).size();
        }
        String sourceFile = (String) task.getInputData().getOrDefault("sourceFile", "unknown");
        System.out.println("  [report] Generated report for " + sourceFile);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("report", Map.of("file", sourceFile, "totalTests", total, "coverage", "87%", "status", "ready"));
        return result;
    }
}
