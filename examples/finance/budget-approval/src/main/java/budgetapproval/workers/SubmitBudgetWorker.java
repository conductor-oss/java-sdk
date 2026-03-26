package budgetapproval.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class SubmitBudgetWorker implements Worker {
    @Override public String getTaskDefName() { return "bgt_submit_budget"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [submit] Budget " + task.getInputData().get("budgetId") + " for " + task.getInputData().get("department") + ": $" + task.getInputData().get("amount"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("submissionId", "SUB-504-001"); r.getOutputData().put("submittedAt", "2026-03-08T09:00:00Z"); r.getOutputData().put("fiscalQuarter", "Q2-2026");
        return r;
    }
}
