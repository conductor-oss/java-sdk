package userfeedback.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ClassifyFeedbackWorker implements Worker {
    @Override public String getTaskDefName() { return "ufb_classify"; }
    @Override public TaskResult execute(Task task) {
        String text = ((String) task.getInputData().getOrDefault("feedbackText", "")).toLowerCase();
        String category = text.contains("bug") ? "bug" : text.contains("feature") ? "feature_request" : "general";
        String priority = text.contains("urgent") || text.contains("crash") ? "high" : "medium";
        System.out.println("  [classify] Category: " + category + ", Priority: " + priority);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("category", category);
        r.getOutputData().put("priority", priority);
        r.getOutputData().put("sentiment", "neutral");
        return r;
    }
}
