package eventdedup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComputeHashWorkerTest {

    private final ComputeHashWorker worker = new ComputeHashWorker();

    @Test
    void taskDefName() {
        assertEquals("dd_compute_hash", worker.getTaskDefName());
    }

    @Test
    void returnsFixedHash() {
        Task task = taskWith(Map.of(
                "eventId", "evt-1001",
                "payload", Map.of("type", "order.created")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("sha256_fixed_hash_001", result.getOutputData().get("hash"));
    }

    @Test
    void hashIsDeterministic() {
        Task task1 = taskWith(Map.of(
                "eventId", "evt-1001",
                "payload", Map.of("type", "order.created")));
        Task task2 = taskWith(Map.of(
                "eventId", "evt-1001",
                "payload", Map.of("type", "order.created")));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("hash"), result2.getOutputData().get("hash"));
    }

    @Test
    void hashSameForDifferentEvents() {
        Task task1 = taskWith(Map.of(
                "eventId", "evt-1001",
                "payload", Map.of("type", "order.created")));
        Task task2 = taskWith(Map.of(
                "eventId", "evt-2002",
                "payload", Map.of("type", "payment.received")));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        // Fixed hash — always the same regardless of input
        assertEquals("sha256_fixed_hash_001", result1.getOutputData().get("hash"));
        assertEquals("sha256_fixed_hash_001", result2.getOutputData().get("hash"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("payload", Map.of("type", "order.created"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("sha256_fixed_hash_001", result.getOutputData().get("hash"));
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-1001");
        input.put("payload", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("sha256_fixed_hash_001", result.getOutputData().get("hash"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("sha256_fixed_hash_001", result.getOutputData().get("hash"));
    }

    @Test
    void outputContainsOnlyHash() {
        Task task = taskWith(Map.of(
                "eventId", "evt-1001",
                "payload", Map.of("type", "order.created")));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("hash"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
