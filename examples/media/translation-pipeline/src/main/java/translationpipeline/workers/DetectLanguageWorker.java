package translationpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class DetectLanguageWorker implements Worker {
    @Override public String getTaskDefName() { return "trn_detect_language"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [detect] Processing " + task.getInputData().getOrDefault("detectedLanguage", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("detectedLanguage", "en");
        r.getOutputData().put("confidence", 0.99);
        r.getOutputData().put("alternativeLanguages", List.of("en-GB"));
        r.getOutputData().put("score", 0.85);
        return r;
    }
}
