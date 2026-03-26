package eventdedup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessEventWorkerTest {

    private final ProcessEventWorker worker = new ProcessEventWorker();

    @Test
    void taskDefName() {
        assertEquals("dd_process_event", worker.getTaskDefName());
    }

    @Test
    void processesEvent() {
        Task task = taskWith(Map.of(
                "eventId", "evt-1001",
                "payload", Map.of("type", "order.created"),
                "hash", "sha256_fixed_hash_001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("evt-1001", result.getOutputData().get("eventId"));
    }

    @Test
    void returnsProcessedTrue() {
        Task task = taskWith(Map.of(
                "eventId", "evt-2002",
                "payload", Map.of("type", "payment.received"),
                "hash", "sha256_fixed_hash_001"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void returnsCorrectEventId() {
        Task task = taskWith(Map.of(
                "eventId", "evt-3003",
                "payload", Map.of("type", "user.created"),
                "hash", "sha256_fixed_hash_001"));
        TaskResult result = worker.execute(task);

        assertEquals("evt-3003", result.getOutputData().get("eventId"));
    }

    @Test
    void outputContainsProcessedAndEventId() {
        Task task = taskWith(Map.of(
                "eventId", "evt-1001",
                "payload", Map.of("type", "order.created"),
                "hash", "sha256_fixed_hash_001"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("processed"));
        assertTrue(result.getOutputData().containsKey("eventId"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("payload", Map.of("type", "order.created"));
        input.put("hash", "sha256_fixed_hash_001");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertNull(result.getOutputData().get("eventId"));
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-1001");
        input.put("payload", null);
        input.put("hash", "sha256_fixed_hash_001");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
