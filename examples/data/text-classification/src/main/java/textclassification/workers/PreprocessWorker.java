package textclassification.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class PreprocessWorker implements Worker {
    @Override public String getTaskDefName() { return "txc_preprocess"; }
    @Override public TaskResult execute(Task task) {
        String text = (String) task.getInputData().getOrDefault("text", "");
        String cleaned = text.toLowerCase().replaceAll("[^\\w\\s]", "").trim();
        System.out.println("  [preprocess] Text cleaned (" + text.length() + " -> " + cleaned.length() + " chars)");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cleanedText", cleaned);
        result.getOutputData().put("tokenCount", cleaned.split("\\s+").length);
        return result;
    }
}
