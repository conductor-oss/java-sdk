package scholarshipprocessing.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NotifyWorker implements Worker {
    @Override public String getTaskDefName() { return "scp_notify"; }
    @Override public TaskResult execute(Task task) {
        boolean awarded = Boolean.TRUE.equals(task.getInputData().get("awarded"));
        String msg = awarded ? "Awarded $" + task.getInputData().get("amount") : "Not selected";
        System.out.println("  [notify] " + task.getInputData().get("studentId") + ": " + msg);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        result.getOutputData().put("message", msg);
        return result;
    }
}
