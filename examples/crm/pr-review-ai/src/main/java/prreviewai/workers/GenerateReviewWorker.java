package prreviewai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class GenerateReviewWorker implements Worker {
    @Override public String getTaskDefName() { return "prr_generate_review"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        Map<String, Object> a = (Map<String, Object>) task.getInputData().getOrDefault("analysis", Map.of());
        List<Map<String, Object>> issues = (List<Map<String, Object>>) a.getOrDefault("issues", List.of());
        boolean hasSecurity = issues.stream().anyMatch(i -> "security".equals(i.get("type")));
        String verdict = hasSecurity ? "request_changes" : "approve";
        System.out.println("  [review] Verdict: " + verdict);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("review", Map.of("comments", issues, "verdict", verdict));
        result.getOutputData().put("verdict", verdict);
        return result;
    }
}
