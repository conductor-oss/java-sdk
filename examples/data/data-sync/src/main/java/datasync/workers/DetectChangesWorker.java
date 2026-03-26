package datasync.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Detects changes in both systems and identifies conflicts.
 * Input: systemA, systemB
 * Output: changesInA, changesInB, conflicts, changeCountA, changeCountB, conflictCount
 */
public class DetectChangesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sy_detect_changes";
    }

    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> changesInA = List.of(
                Map.of("recordId", "R001", "field", "email", "oldValue", "alice@old.com", "newValue", "alice@new.com", "modifiedAt", "2024-03-15T10:00:00Z"),
                Map.of("recordId", "R002", "field", "status", "oldValue", "active", "newValue", "suspended", "modifiedAt", "2024-03-15T10:30:00Z"),
                Map.of("recordId", "R003", "field", "name", "oldValue", "Robert", "newValue", "Bob", "modifiedAt", "2024-03-15T11:00:00Z")
        );

        List<Map<String, Object>> changesInB = List.of(
                Map.of("recordId", "R001", "field", "phone", "oldValue", "555-0100", "newValue", "555-0199", "modifiedAt", "2024-03-15T10:15:00Z"),
                Map.of("recordId", "R002", "field", "status", "oldValue", "active", "newValue", "inactive", "modifiedAt", "2024-03-15T10:45:00Z"),
                Map.of("recordId", "R004", "field", "address", "oldValue", "123 Main St", "newValue", "456 Oak Ave", "modifiedAt", "2024-03-15T09:00:00Z")
        );

        List<Map<String, Object>> conflicts = List.of(
                Map.of("recordId", "R002", "field", "status", "valueA", "suspended", "valueB", "inactive",
                        "modifiedA", "2024-03-15T10:30:00Z", "modifiedB", "2024-03-15T10:45:00Z")
        );

        System.out.println("  [detect] Changes: " + changesInA.size() + " in A, "
                + changesInB.size() + " in B, " + conflicts.size() + " conflict(s)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("changesInA", changesInA);
        result.getOutputData().put("changesInB", changesInB);
        result.getOutputData().put("conflicts", conflicts);
        result.getOutputData().put("changeCountA", changesInA.size());
        result.getOutputData().put("changeCountB", changesInB.size());
        result.getOutputData().put("conflictCount", conflicts.size());
        return result;
    }
}
