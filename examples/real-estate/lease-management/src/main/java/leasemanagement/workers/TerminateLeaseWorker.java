package leasemanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class TerminateLeaseWorker implements Worker {
    @Override public String getTaskDefName() { return "lse_terminate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [lse_terminate] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("terminated", "true");
        return result;
    }
}
