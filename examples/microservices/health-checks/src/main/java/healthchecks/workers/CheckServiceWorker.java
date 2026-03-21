package healthchecks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.Map;

/**
 * Checks health of an individual service.
 * Input: serviceName, endpoint
 * Output: health (object with service, status, latencyMs, uptime, lastChecked)
 */
public class CheckServiceWorker implements Worker {

    private static final Map<String, String> STATUSES = Map.of(
            "api-gateway", "healthy", "database", "healthy", "redis-cache", "healthy");
    private static final Map<String, Integer> LATENCIES = Map.of(
            "api-gateway", 12, "database", 8, "redis-cache", 3);

    @Override
    public String getTaskDefName() {
        return "hc_check_service";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        String endpoint = (String) task.getInputData().get("endpoint");
        if (serviceName == null) serviceName = "unknown";
        if (endpoint == null) endpoint = "unknown";

        System.out.println("  [health] Checking " + serviceName + " at " + endpoint + "...");

        String status = STATUSES.getOrDefault(serviceName, "unknown");
        int latency = LATENCIES.getOrDefault(serviceName, 0);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("health", Map.of(
                "service", serviceName,
                "status", status,
                "latencyMs", latency,
                "uptime", "99.97%",
                "lastChecked", Instant.now().toString()));
        return result;
    }
}
