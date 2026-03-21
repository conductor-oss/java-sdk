package serviceregistry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Discovers a service's endpoint from the registry.
 * Input: serviceName, registrationId, healthy
 * Output: endpoint, instances
 */
public class DiscoverServiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sr_discover_service";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        if (serviceName == null) {
            serviceName = "unknown";
        }

        Object healthyObj = task.getInputData().get("healthy");
        boolean healthy = healthyObj != null && Boolean.parseBoolean(healthyObj.toString());

        System.out.println("  [discover] Found " + serviceName + " (healthy: " + healthy + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("endpoint", serviceName + ".internal:8080");
        result.getOutputData().put("instances", 3);
        return result;
    }
}
