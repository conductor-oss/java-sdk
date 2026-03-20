package bugtriage.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class HandleCriticalWorker implements Worker {
    @Override public String getTaskDefName() { return "btg_handle_critical"; }
    @Override public TaskResult execute(Task task) {
        String bugId = (String) task.getInputData().getOrDefault("bugId", "unknown");
        System.out.println("  [critical] Escalating " + bugId + " — paging on-call");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handled", true);
        result.getOutputData().put("action", "paged_oncall");
        return result;
    }
}
