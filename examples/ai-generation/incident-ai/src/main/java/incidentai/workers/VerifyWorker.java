package incidentai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class VerifyWorker implements Worker {
    @Override public String getTaskDefName() { return "iai_verify"; }
    @Override public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().getOrDefault("serviceName", "unknown");
        System.out.println("  [verify] Verifying " + serviceName + " health post-fix");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", true);
        result.getOutputData().put("errorRate", 0.001);
        result.getOutputData().put("status", "healthy");
        return result;
    }
}
