package healthchecks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Generates a health report from individual service checks.
 * Input: apiHealth, dbHealth, cacheHealth
 * Output: overallStatus, services, checkedAt
 */
public class GenerateReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hc_generate_report";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> apiHealth = (Map<String, Object>) task.getInputData().get("apiHealth");
        Map<String, Object> dbHealth = (Map<String, Object>) task.getInputData().get("dbHealth");
        Map<String, Object> cacheHealth = (Map<String, Object>) task.getInputData().get("cacheHealth");

        List<Map<String, Object>> services = List.of(
                apiHealth != null ? apiHealth : Map.of(),
                dbHealth != null ? dbHealth : Map.of(),
                cacheHealth != null ? cacheHealth : Map.of());

        boolean allHealthy = services.stream()
                .allMatch(s -> "healthy".equals(s.get("status")));

        String overall = allHealthy ? "healthy" : "degraded";
        System.out.println("  [report] Overall: " + (allHealthy ? "ALL HEALTHY" : "DEGRADED") + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("overallStatus", overall);
        result.getOutputData().put("services", services);
        result.getOutputData().put("checkedAt", Instant.now().toString());
        return result;
    }
}
