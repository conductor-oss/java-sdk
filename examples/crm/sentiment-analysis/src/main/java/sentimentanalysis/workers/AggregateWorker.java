package sentimentanalysis.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class AggregateWorker implements Worker {
    @Override public String getTaskDefName() { return "snt_aggregate"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> cls = (List<Map<String, Object>>) task.getInputData().getOrDefault("classifications", List.of());
        long pos = cls.stream().filter(c -> "positive".equals(c.get("label"))).count();
        long neg = cls.stream().filter(c -> "negative".equals(c.get("label"))).count();
        long neu = cls.stream().filter(c -> "neutral".equals(c.get("label"))).count();
        String overall = pos > neg ? "positive" : neg > pos ? "negative" : "neutral";
        System.out.println("  [aggregate] Overall: " + overall + " - positive: " + pos + ", negative: " + neg + ", neutral: " + neu);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("overallSentiment", overall);
        result.getOutputData().put("distribution", Map.of("positive", pos, "negative", neg, "neutral", neu));
        result.getOutputData().put("source", task.getInputData().get("source"));
        return result;
    }
}
