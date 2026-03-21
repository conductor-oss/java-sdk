package translationpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReviewTranslationWorker implements Worker {
    @Override public String getTaskDefName() { return "trn_review_translation"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [review] Processing " + task.getInputData().getOrDefault("approved", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("approved", true);
        r.getOutputData().put("finalText", task.getInputData().get("translatedText"));
        r.getOutputData().put("reviewerId", "TRANSLATOR-FR-01");
        r.getOutputData().put("corrections", 0);
        r.getOutputData().put("reviewScore", 9.2);
        return r;
    }
}
