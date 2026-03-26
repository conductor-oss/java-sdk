package bugtriage.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class HandleLowWorker implements Worker {
    @Override public String getTaskDefName() { return "btg_handle_low"; }
    @Override public TaskResult execute(Task task) {
        String bugId = (String) task.getInputData().getOrDefault("bugId", "unknown");
        System.out.println("  [low] Adding " + bugId + " to backlog");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handled", true);
        result.getOutputData().put("action", "backlog");
        return result;
    }
}
