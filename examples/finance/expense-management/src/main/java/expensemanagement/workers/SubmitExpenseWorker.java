package expensemanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class SubmitExpenseWorker implements Worker {
    @Override public String getTaskDefName() { return "exp_submit_expense"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [submit] Expense " + task.getInputData().get("expenseId") + " by " + task.getInputData().get("employeeId") + ": $" + task.getInputData().get("amount"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("submittedAt", "2026-03-08T08:00:00Z"); r.getOutputData().put("referenceNumber", "EXP-505-001");
        return r;
    }
}
