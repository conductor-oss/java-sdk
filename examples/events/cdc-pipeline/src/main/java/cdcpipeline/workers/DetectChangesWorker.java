package cdcpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Detects CDC changes from a source table since a given timestamp.
 * Returns a fixed set of 4 change records: INSERT, UPDATE, DELETE, INSERT.
 */
public class DetectChangesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cd_detect_changes";
    }

    @Override
    public TaskResult execute(Task task) {
        String sourceTable = (String) task.getInputData().get("sourceTable");
        if (sourceTable == null || sourceTable.isBlank()) {
            sourceTable = "users";
        }

        String sinceTimestamp = (String) task.getInputData().get("sinceTimestamp");
        if (sinceTimestamp == null || sinceTimestamp.isBlank()) {
            sinceTimestamp = "2026-03-08T10:00:00Z";
        }

        System.out.println("  [cd_detect_changes] Scanning table '" + sourceTable + "' since " + sinceTimestamp);

        List<Map<String, Object>> changes = List.of(
                Map.of("operation", "INSERT", "table", sourceTable, "timestamp", "2026-03-08T10:01:00Z",
                        "data", Map.of("id", 1001, "name", "Alice", "plan", "starter")),
                Map.of("operation", "UPDATE", "table", sourceTable, "timestamp", "2026-03-08T10:02:00Z",
                        "data", Map.of("id", 998, "name", "Dave", "plan", "enterprise"),
                        "previousData", Map.of("id", 998, "name", "Dave", "plan", "pro")),
                Map.of("operation", "DELETE", "table", sourceTable, "timestamp", "2026-03-08T10:03:00Z",
                        "data", Map.of("id", 872, "name", "Bob")),
                Map.of("operation", "INSERT", "table", sourceTable, "timestamp", "2026-03-08T10:04:00Z",
                        "data", Map.of("id", 1002, "name", "Carol", "plan", "starter"))
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("changes", changes);
        result.getOutputData().put("changeCount", 4);
        result.getOutputData().put("lastTimestamp", "2026-03-08T10:04:00Z");
        return result;
    }
}
