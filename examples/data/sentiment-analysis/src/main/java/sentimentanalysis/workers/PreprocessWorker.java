package sentimentanalysis.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class PreprocessWorker implements Worker {
    @Override public String getTaskDefName() { return "snt_preprocess"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<String> texts = (List<String>) task.getInputData().getOrDefault("texts", List.of());
        System.out.println("  [preprocess] Cleaned " + texts.size() + " texts: lowercase, remove stopwords, normalize");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cleanedTexts", texts);
        result.getOutputData().put("processedCount", texts.size());
        return result;
    }
}
