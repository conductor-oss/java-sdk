package sentimentanalysis.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class ClassifyWorker implements Worker {
    @Override public String getTaskDefName() { return "snt_classify"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> sentiments = (List<Map<String, Object>>) task.getInputData().getOrDefault("sentiments", List.of());
        List<Map<String, Object>> classifications = sentiments.stream()
            .map(s -> Map.<String, Object>of("text", s.get("text"), "label", s.get("label"), "confidence", Math.abs(((Number) s.get("score")).doubleValue())))
            .toList();
        System.out.println("  [classify] Classifications: " + classifications.stream().map(c -> (String) c.get("label")).reduce((a, b) -> a + ", " + b).orElse(""));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("classifications", classifications);
        return result;
    }
}
