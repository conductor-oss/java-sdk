package budgetapproval.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReviewBudgetWorker implements Worker {
    @Override public String getTaskDefName() { return "bgt_review_budget"; }
    @Override public TaskResult execute(Task task) {
        Object amtObj = task.getInputData().get("amount"); double amount = amtObj instanceof Number ? ((Number)amtObj).doubleValue() : 0;
        String decision = amount <= 50000 ? "approve" : "revise";
        System.out.println("  [review] Budget $" + amount + " — decision: " + decision);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("decision", decision); r.getOutputData().put("reviewer", "CFO-Martinez");
        r.getOutputData().put("revisedAmount", "revise".equals(decision) ? 50000 : amount);
        r.getOutputData().put("comments", "revise".equals(decision) ? "Please reduce to Q2 cap" : "Within budget limits");
        return r;
    }
}
