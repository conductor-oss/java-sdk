package textclassification.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ConfidenceWorker implements Worker {
    @Override public String getTaskDefName() { return "txc_confidence"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        Map<String, Object> scores = (Map<String, Object>) task.getInputData().getOrDefault("scores", Map.of());
        double max = scores.values().stream().filter(v -> v instanceof Number).mapToDouble(v -> ((Number) v).doubleValue()).max().orElse(0);
        double second = scores.values().stream().filter(v -> v instanceof Number).mapToDouble(v -> ((Number) v).doubleValue()).sorted().skip(Math.max(0, scores.size() - 2)).findFirst().orElse(0);
        double margin = max - second;
        boolean reliable = max > 0.7 && margin > 0.1;
        System.out.println("  [confidence] Confidence: " + max + ", margin: " + String.format("%.2f", margin) + ", reliable: " + reliable);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("confidence", max);
        result.getOutputData().put("margin", margin);
        result.getOutputData().put("reliable", reliable);
        return result;
    }
}
