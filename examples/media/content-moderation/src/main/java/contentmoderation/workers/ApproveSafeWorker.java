package contentmoderation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ApproveSafeWorker implements Worker {
    @Override public String getTaskDefName() { return "mod_approve_safe"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [safe] Processing " + task.getInputData().getOrDefault("approved", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("approved", true);
        r.getOutputData().put("approvedAt", "2026-03-08T10:01:00Z");
        return r;
    }
}
