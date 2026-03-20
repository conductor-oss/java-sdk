package mortgageapplication.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CloseWorker implements Worker {
    @Override public String getTaskDefName() { return "mtg_close"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [mtg_close] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("closingStatus", "funded");
        return result;
    }
}
