package mortgageapplication.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class UnderwriteWorker implements Worker {
    @Override public String getTaskDefName() { return "mtg_underwrite"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [mtg_underwrite] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("underwriting", "approved");
        return result;
    }
}
