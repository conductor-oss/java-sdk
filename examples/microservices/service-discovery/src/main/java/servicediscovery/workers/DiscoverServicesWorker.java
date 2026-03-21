package servicediscovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Discovers service instances from a registry.
 * Input: serviceName
 * Output: instances, registrySource
 */
public class DiscoverServicesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sd_discover_services";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        if (serviceName == null) serviceName = "unknown-service";

        System.out.println("  [sd_discover_services] Looking up instances for \"" + serviceName + "\"...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("instances", List.of(
                Map.of("id", "inst-01", "host", "10.0.1.10", "port", 8080, "health", "healthy", "connections", 12),
                Map.of("id", "inst-02", "host", "10.0.1.11", "port", 8080, "health", "healthy", "connections", 5),
                Map.of("id", "inst-03", "host", "10.0.1.12", "port", 8080, "health", "degraded", "connections", 30)
        ));
        result.getOutputData().put("registrySource", "consul");
        return result;
    }
}
