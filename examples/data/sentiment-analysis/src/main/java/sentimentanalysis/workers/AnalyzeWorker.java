package sentimentanalysis.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class AnalyzeWorker implements Worker {
    @Override public String getTaskDefName() { return "snt_analyze"; }
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> sentiments = List.of(
            Map.of("text", "Great product!", "score", 0.92, "label", "positive"),
            Map.of("text", "Terrible support", "score", -0.78, "label", "negative"),
            Map.of("text", "It's okay", "score", 0.12, "label", "neutral")
        );
        System.out.println("  [analyze] Sentiment scores computed for " + sentiments.size() + " texts");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sentiments", sentiments);
        return result;
    }
}
