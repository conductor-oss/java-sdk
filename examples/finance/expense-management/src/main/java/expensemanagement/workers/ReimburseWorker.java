package expensemanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReimburseWorker implements Worker {
    @Override public String getTaskDefName() { return "exp_reimburse"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [reimburse] Reimbursing $" + task.getInputData().get("approvedAmount") + " to " + task.getInputData().get("employeeId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reimbursementStatus", "processed"); r.getOutputData().put("paymentId", "PAY-505-001"); r.getOutputData().put("expectedDate", "2026-03-12");
        return r;
    }
}
