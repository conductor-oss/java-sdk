package leasemanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class RenewLeaseWorker implements Worker {
    @Override public String getTaskDefName() { return "lse_renew"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [lse_renew] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("renewed", "true");
        return result;
    }
}
