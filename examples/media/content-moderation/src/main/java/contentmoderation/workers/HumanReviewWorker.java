package contentmoderation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class HumanReviewWorker implements Worker {
    @Override public String getTaskDefName() { return "mod_human_review"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [review] Processing " + task.getInputData().getOrDefault("reviewerDecision", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reviewerDecision", "approve_with_warning");
        r.getOutputData().put("reviewerId", "MOD-42");
        r.getOutputData().put("reviewedAt", "2026-03-08T10:15:00Z");
        r.getOutputData().put("notes", "Borderline content");
        return r;
    }
}
