package budgetapproval.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class RejectBudgetWorker implements Worker {
    @Override public String getTaskDefName() { return "bgt_reject_budget"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [reject] Budget " + task.getInputData().get("budgetId") + " rejected: " + task.getInputData().get("reason"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("rejected", true); r.getOutputData().put("rejectionId", "REJ-504-001");
        return r;
    }
}
