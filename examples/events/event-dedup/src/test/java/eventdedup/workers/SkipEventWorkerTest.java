package eventdedup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SkipEventWorkerTest {

    private final SkipEventWorker worker = new SkipEventWorker();

    @Test
    void taskDefName() {
        assertEquals("dd_skip_event", worker.getTaskDefName());
    }

    @Test
    void skipsEvent() {
        Task task = taskWith(Map.of(
                "eventId", "evt-1001",
                "hash", "sha256_fixed_hash_001",
                "reason", "Event already processed (duplicate detected)"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("skipped"));
        assertEquals("evt-1001", result.getOutputData().get("eventId"));
    }

    @Test
    void returnsSkippedTrue() {
        Task task = taskWith(Map.of(
                "eventId", "evt-2002",
                "hash", "sha256_fixed_hash_001",
                "reason", "duplicate"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("skipped"));
    }

    @Test
    void returnsCorrectEventId() {
        Task task = taskWith(Map.of(
                "eventId", "evt-3003",
                "hash", "sha256_fixed_hash_001",
                "reason", "duplicate"));
        TaskResult result = worker.execute(task);

        assertEquals("evt-3003", result.getOutputData().get("eventId"));
    }

    @Test
    void returnsReason() {
        String reason = "Event already processed (duplicate detected)";
        Task task = taskWith(Map.of(
                "eventId", "evt-1001",
                "hash", "sha256_fixed_hash_001",
                "reason", reason));
        TaskResult result = worker.execute(task);

        assertEquals(reason, result.getOutputData().get("reason"));
    }

    @Test
    void outputContainsSkippedEventIdAndReason() {
        Task task = taskWith(Map.of(
                "eventId", "evt-1001",
                "hash", "sha256_fixed_hash_001",
                "reason", "duplicate"));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("skipped"));
        assertTrue(result.getOutputData().containsKey("eventId"));
        assertTrue(result.getOutputData().containsKey("reason"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("hash", "sha256_fixed_hash_001");
        input.put("reason", "duplicate");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("skipped"));
        assertNull(result.getOutputData().get("eventId"));
    }

    @Test
    void handlesNullReason() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-1001");
        input.put("hash", "sha256_fixed_hash_001");
        input.put("reason", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("skipped"));
        assertNull(result.getOutputData().get("reason"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("skipped"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
