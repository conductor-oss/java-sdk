package expensemanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CategorizeWorker implements Worker {
    @Override public String getTaskDefName() { return "exp_categorize"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [categorize] Expense " + task.getInputData().get("expenseId") + " -> " + task.getInputData().get("category"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("finalCategory", "office_supplies"); r.getOutputData().put("glCode", "6200"); r.getOutputData().put("taxDeductible", true);
        return r;
    }
}
