package incidentai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ExecuteFixWorker implements Worker {
    @Override public String getTaskDefName() { return "iai_execute_fix"; }
    @Override public TaskResult execute(Task task) {
        String fix = (String) task.getInputData().getOrDefault("fix", "unknown");
        System.out.println("  [execute] Applying fix: " + fix);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("fixApplied", true);
        result.getOutputData().put("action", "gateway_failover");
        return result;
    }
}
