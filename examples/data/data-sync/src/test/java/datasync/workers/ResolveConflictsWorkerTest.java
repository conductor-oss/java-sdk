package datasync.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResolveConflictsWorkerTest {

    private final ResolveConflictsWorker worker = new ResolveConflictsWorker();

    @Test
    void taskDefName() {
        assertEquals("sy_resolve_conflicts", worker.getTaskDefName());
    }

    @Test
    void resolvesConflicts() {
        Map<String, Object> input = new HashMap<>();
        input.put("changesInA", List.of(
                Map.of("recordId", "R002", "field", "status", "newValue", "suspended")));
        input.put("changesInB", List.of(
                Map.of("recordId", "R002", "field", "status", "newValue", "inactive")));
        input.put("conflicts", List.of(
                Map.of("recordId", "R002", "field", "status", "valueA", "suspended", "valueB", "inactive",
                        "modifiedA", "2024-03-15T10:30:00Z", "modifiedB", "2024-03-15T10:45:00Z")));
        input.put("conflictStrategy", "latest_wins");

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("resolvedCount"));
    }

    @Test
    void handlesNoConflicts() {
        Map<String, Object> input = new HashMap<>();
        input.put("changesInA", List.of(Map.of("recordId", "R001", "field", "email")));
        input.put("changesInB", List.of(Map.of("recordId", "R004", "field", "address")));
        input.put("conflicts", List.of());

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("resolvedCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void latestWinsPicksSystemB() {
        Map<String, Object> input = new HashMap<>();
        input.put("changesInA", List.of());
        input.put("changesInB", List.of());
        input.put("conflicts", List.of(
                Map.of("recordId", "R002", "field", "status", "valueA", "suspended", "valueB", "inactive",
                        "modifiedA", "2024-03-15T10:00:00Z", "modifiedB", "2024-03-15T11:00:00Z")));

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> resolved = (List<Map<String, Object>>) result.getOutputData().get("resolved");
        assertEquals("inactive", resolved.get(0).get("resolvedValue"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
