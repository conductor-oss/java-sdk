package escrowmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DepositWorker implements Worker {
    @Override public String getTaskDefName() { return "esc_deposit"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [esc_deposit] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deposited", "true");
        return result;
    }
}
