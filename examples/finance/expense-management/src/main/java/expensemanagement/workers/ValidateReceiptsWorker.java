package expensemanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ValidateReceiptsWorker implements Worker {
    @Override public String getTaskDefName() { return "exp_validate_receipts"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] Checking receipt for expense " + task.getInputData().get("expenseId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("valid", true); r.getOutputData().put("receiptDate", "2026-03-05");
        r.getOutputData().put("merchantName", "Office Depot"); r.getOutputData().put("ocrConfidence", 0.97);
        return r;
    }
}
