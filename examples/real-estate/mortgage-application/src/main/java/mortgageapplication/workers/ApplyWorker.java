package mortgageapplication.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ApplyWorker implements Worker {
    @Override public String getTaskDefName() { return "mtg_apply"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [mtg_apply] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("applicationId", "MTG-683");
        return result;
    }
}
