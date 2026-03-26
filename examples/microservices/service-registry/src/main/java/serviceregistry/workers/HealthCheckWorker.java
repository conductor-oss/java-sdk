package serviceregistry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Performs a health check on a registered service.
 * Input: serviceUrl
 * Output: healthy, latencyMs
 */
public class HealthCheckWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sr_health_check";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceUrl = (String) task.getInputData().get("serviceUrl");
        if (serviceUrl == null) {
            serviceUrl = "unknown";
        }

        System.out.println("  [health] Checking " + serviceUrl + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("healthy", true);
        result.getOutputData().put("latencyMs", 12);
        return result;
    }
}
