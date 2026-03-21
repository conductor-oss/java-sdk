package userfeedback.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.UUID;

public class CollectFeedbackWorker implements Worker {
    @Override public String getTaskDefName() { return "ufb_collect"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Feedback received from " + task.getInputData().get("userId") + " via " + task.getInputData().get("source"));
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("feedbackId", "FB-" + UUID.randomUUID().toString().substring(0, 8));
        r.getOutputData().put("receivedAt", Instant.now().toString());
        return r;
    }
}
