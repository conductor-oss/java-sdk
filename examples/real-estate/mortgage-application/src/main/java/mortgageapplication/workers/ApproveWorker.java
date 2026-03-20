package mortgageapplication.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ApproveWorker implements Worker {
    @Override public String getTaskDefName() { return "mtg_approve"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [mtg_approve] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("loanId", "LOAN-2024-683");
        return result;
    }
}
