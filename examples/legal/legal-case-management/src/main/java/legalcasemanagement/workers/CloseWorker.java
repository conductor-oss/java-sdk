package legalcasemanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CloseWorker implements Worker {
    @Override public String getTaskDefName() { return "lcm_close"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [lcm_close] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("outcome", "settled");
        return result;
    }
}
