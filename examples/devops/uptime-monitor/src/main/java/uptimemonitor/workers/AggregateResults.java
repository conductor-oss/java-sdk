package uptimemonitor.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Aggregates results from parallel endpoint checks and determines overall status.
 */
public class AggregateResults implements Worker {

    @Override
    public String getTaskDefName() {
        return "uptime_aggregate_results";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[uptime_aggregate_results] Aggregating endpoint check results...");

        TaskResult result = new TaskResult(task);
        Map<String, Object> joinOutput = (Map<String, Object>) task.getInputData().get("joinOutput");

        int healthy = 0, degraded = 0, down = 0;
        long totalResponseTime = 0;
        int responseCount = 0;
        List<Map<String, Object>> failures = new ArrayList<>();

        if (joinOutput != null) {
            for (Map.Entry<String, Object> entry : joinOutput.entrySet()) {
                if (!entry.getKey().startsWith("check_ep_")) continue;

                Map<String, Object> epResult = (Map<String, Object>) entry.getValue();
                String status = String.valueOf(epResult.get("status"));

                switch (status) {
                    case "healthy":
                        healthy++;
                        break;
                    case "degraded":
                        degraded++;
                        break;
                    default:
                        down++;
                        break;
                }

                Object rtObj = epResult.get("responseTimeMs");
                if (rtObj instanceof Number) {
                    totalResponseTime += ((Number) rtObj).longValue();
                    responseCount++;
                }

                if (!"healthy".equals(status)) {
                    Map<String, Object> failure = new LinkedHashMap<>();
                    failure.put("name", epResult.get("name"));
                    failure.put("url", epResult.get("url"));
                    failure.put("status", status);
                    failure.put("failedChecks", epResult.get("failedChecks"));
                    failures.add(failure);
                }
            }
        }

        int total = healthy + degraded + down;
        String overallStatus;
        if (down > 0) {
            overallStatus = "critical";
        } else if (degraded > 0) {
            overallStatus = "degraded";
        } else {
            overallStatus = "healthy";
        }

        long avgResponseTime = responseCount > 0 ? totalResponseTime / responseCount : 0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalEndpoints", total);
        summary.put("healthy", healthy);
        summary.put("degraded", degraded);
        summary.put("down", down);
        summary.put("avgResponseTimeMs", avgResponseTime);

        System.out.println("  Total: " + total + " | Healthy: " + healthy
                + " | Degraded: " + degraded + " | Down: " + down);
        System.out.println("  Overall: " + overallStatus.toUpperCase());

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("overallStatus", overallStatus);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("hasFailures", !failures.isEmpty());
        result.getOutputData().put("failures", failures);
        return result;
    }
}
