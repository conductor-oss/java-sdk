package logprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Ingests raw log entries from a specified source.
 * Input: logSource, timeRange
 * Output: rawLogs, entryCount
 */
public class IngestLogsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lp_ingest_logs";
    }

    @Override
    public TaskResult execute(Task task) {
        String source = (String) task.getInputData().getOrDefault("logSource", "unknown");

        List<Map<String, Object>> rawLogs = List.of(
                Map.of("ts", "2024-03-15T10:00:01Z", "level", "INFO", "service", "api-gateway", "msg", "Request received GET /api/users"),
                Map.of("ts", "2024-03-15T10:00:02Z", "level", "ERROR", "service", "auth-service", "msg", "Token validation failed: expired"),
                Map.of("ts", "2024-03-15T10:00:03Z", "level", "WARN", "service", "api-gateway", "msg", "Rate limit approaching for client C-100"),
                Map.of("ts", "2024-03-15T10:00:04Z", "level", "INFO", "service", "user-service", "msg", "User profile fetched successfully"),
                Map.of("ts", "2024-03-15T10:00:05Z", "level", "ERROR", "service", "auth-service", "msg", "Token validation failed: expired"),
                Map.of("ts", "2024-03-15T10:00:06Z", "level", "INFO", "service", "api-gateway", "msg", "Request received POST /api/orders"),
                Map.of("ts", "2024-03-15T10:00:07Z", "level", "ERROR", "service", "order-service", "msg", "Database connection timeout")
        );

        System.out.println("  [ingest] Ingested " + rawLogs.size() + " log entries from " + source);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rawLogs", rawLogs);
        result.getOutputData().put("entryCount", rawLogs.size());
        return result;
    }
}
