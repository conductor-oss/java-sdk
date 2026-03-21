package budgetapproval.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ApproveBudgetWorker implements Worker {
    @Override public String getTaskDefName() { return "bgt_approve_budget"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [approve] Budget " + task.getInputData().get("budgetId") + " approved by " + task.getInputData().get("approver"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("approved", true); r.getOutputData().put("approvalId", "APR-504-001");
        return r;
    }
}
