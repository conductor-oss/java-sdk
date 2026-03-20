package commissioncalculation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class BaseCommissionWorker implements Worker {
    @Override public String getTaskDefName() { return "cmc_base"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [cmc_base] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("baseCommission", "15750");
        return result;
    }
}
