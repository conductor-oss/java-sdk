package leasemanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ActivateLeaseWorker implements Worker {
    @Override public String getTaskDefName() { return "lse_activate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [lse_activate] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("active", "true");
        return result;
    }
}
