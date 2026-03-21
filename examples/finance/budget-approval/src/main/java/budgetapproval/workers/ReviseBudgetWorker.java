package budgetapproval.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReviseBudgetWorker implements Worker {
    @Override public String getTaskDefName() { return "bgt_revise_budget"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [revise] Budget " + task.getInputData().get("budgetId") + " revised to $" + task.getInputData().get("revisedAmount"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("revised", true); r.getOutputData().put("revisedAmount", task.getInputData().get("revisedAmount")); r.getOutputData().put("revisionId", "REV-504-001");
        return r;
    }
}
