package mortgageapplication.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CreditCheckWorker implements Worker {
    @Override public String getTaskDefName() { return "mtg_credit_check"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [mtg_credit_check] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("creditScore", "745");
        return result;
    }
}
