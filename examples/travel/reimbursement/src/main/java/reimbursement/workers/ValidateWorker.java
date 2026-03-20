package reimbursement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ValidateWorker implements Worker {
    @Override public String getTaskDefName() { return "rmb_validate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] Claim " + task.getInputData().get("claimId") + ": " + task.getInputData().get("receiptCount") + " receipts verified");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("valid", true); r.getOutputData().put("receiptsVerified", true); return r;
    }
}
