package userfeedback.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RespondFeedbackWorker implements Worker {
    @Override public String getTaskDefName() { return "ufb_respond"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [respond] Auto-response sent to " + task.getInputData().get("userId"));
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("responseSent", true);
        r.getOutputData().put("message", "Thank you for your " + task.getInputData().get("category") + " feedback. Our " + task.getInputData().get("team") + " team will review it.");
        return r;
    }
}
