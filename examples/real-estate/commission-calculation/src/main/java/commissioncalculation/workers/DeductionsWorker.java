package commissioncalculation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DeductionsWorker implements Worker {
    @Override public String getTaskDefName() { return "cmc_deductions"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [cmc_deductions] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("netCommission", "12127.50");
        return result;
    }
}
