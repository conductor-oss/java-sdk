package eventsourcing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotStateWorkerTest {

    private final SnapshotStateWorker worker = new SnapshotStateWorker();

    @Test
    void taskDefName() {
        assertEquals("ev_snapshot_state", worker.getTaskDefName());
    }

    @Test
    void returnsFixedSnapshotId() {
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "currentState", Map.of("owner", "Alice", "balance", 1550),
                "version", 5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("snap-fixed-001", result.getOutputData().get("snapshotId"));
    }

    @Test
    void returnsFixedSnapshotTimestamp() {
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "currentState", Map.of("owner", "Alice", "balance", 1550),
                "version", 5));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("snapshotAt"));
    }

    @Test
    void returnsVersion() {
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "currentState", Map.of("owner", "Alice", "balance", 1550),
                "version", 5));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("version"));
    }

    @Test
    void returnsAggregateId() {
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "currentState", Map.of("owner", "Alice", "balance", 1550),
                "version", 5));
        TaskResult result = worker.execute(task);

        assertEquals("acct-1001", result.getOutputData().get("aggregateId"));
    }

    @Test
    void returnsState() {
        Map<String, Object> state = Map.of(
                "owner", "Alice",
                "balance", 1550,
                "status", "active",
                "transactionCount", 4,
                "lastDeposit", 250);
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "currentState", state,
                "version", 5));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> outputState = (Map<String, Object>) result.getOutputData().get("state");
        assertNotNull(outputState);
        assertEquals("Alice", outputState.get("owner"));
        assertEquals(1550, outputState.get("balance"));
    }

    @Test
    void handlesNullAggregateId() {
        Map<String, Object> input = new HashMap<>();
        input.put("aggregateId", null);
        input.put("currentState", Map.of("owner", "Alice", "balance", 1550));
        input.put("version", 5);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("snap-fixed-001", result.getOutputData().get("snapshotId"));
    }

    @Test
    void handlesZeroVersion() {
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "currentState", Map.of("balance", 0),
                "version", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("version"));
    }

    @Test
    void handlesMissingVersion() {
        Map<String, Object> input = new HashMap<>();
        input.put("aggregateId", "acct-1001");
        input.put("currentState", Map.of("balance", 0));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("version"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
