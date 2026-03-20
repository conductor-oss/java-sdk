package expensemanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ApproveExpenseWorker implements Worker {
    @Override public String getTaskDefName() { return "exp_approve_expense"; }
    @Override public TaskResult execute(Task task) {
        Object amtObj = task.getInputData().get("amount");
        double amount = amtObj instanceof Number ? ((Number)amtObj).doubleValue() : 0;
        System.out.println("  [approve] Approving $" + amount + " for " + task.getInputData().get("employeeId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("approved", true); r.getOutputData().put("approvedAmount", amount);
        r.getOutputData().put("approver", "MGR-Johnson");
        return r;
    }
}
