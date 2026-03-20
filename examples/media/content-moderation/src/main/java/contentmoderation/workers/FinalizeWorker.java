package contentmoderation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class FinalizeWorker implements Worker {
    @Override public String getTaskDefName() { return "mod_finalize"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [finalize] Processing " + task.getInputData().getOrDefault("finalStatus", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("finalStatus", task.getInputData().get("verdict"));
        r.getOutputData().put("auditLogId", "AUDIT-516-001");
        return r;
    }
}
