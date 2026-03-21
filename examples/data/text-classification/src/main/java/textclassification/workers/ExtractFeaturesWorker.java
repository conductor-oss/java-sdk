package textclassification.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class ExtractFeaturesWorker implements Worker {
    @Override public String getTaskDefName() { return "txc_extract_features"; }
    @Override public TaskResult execute(Task task) {
        String text = (String) task.getInputData().getOrDefault("cleanedText", "");
        String[] words = text.split("\\s+");
        double avgLen = text.replaceAll("\\s", "").length() / (double) Math.max(words.length, 1);
        Map<String, Object> features = Map.of("wordCount", words.length, "hasNumbers", text.matches(".*\\d.*"),
            "avgWordLength", String.format("%.1f", avgLen), "topNgrams", List.of("machine learning", "deep neural", "training data"));
        System.out.println("  [features] Extracted " + features.size() + " feature groups");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("features", features);
        return result;
    }
}
