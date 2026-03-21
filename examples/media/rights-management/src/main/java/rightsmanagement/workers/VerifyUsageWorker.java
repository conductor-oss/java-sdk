package rightsmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyUsageWorker implements Worker {
    @Override public String getTaskDefName() { return "rts_verify_usage"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [verify] Processing " + task.getInputData().getOrDefault("territory", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("territory", task.getInputData().get("territory"));
        r.getOutputData().put("territoryRestriction", false);
        r.getOutputData().put("verifiedAt", "2026-03-08T10:00:00Z");
        return r;
    }
}
