package commissioncalculation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class FinalizeWorker implements Worker {
    @Override public String getTaskDefName() { return "cmc_finalize"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [cmc_finalize] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("paymentId", "PAY-688");
        return result;
    }
}
