package taskdedup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HashInputWorkerTest {

    private final HashInputWorker worker = new HashInputWorker();

    @Test
    void taskDefName() {
        assertEquals("tdd_hash_input", worker.getTaskDefName());
    }

    @Test
    void hashesPayloadSuccessfully() {
        Task task = taskWith(Map.of("payload", Map.of("orderId", "ORD-123", "amount", 99.99)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String hash = (String) result.getOutputData().get("hash");
        assertNotNull(hash);
        assertTrue(hash.startsWith("sha256:"));
    }

    @Test
    void hashIsDeterministic() {
        Task task1 = taskWith(Map.of("payload", Map.of("key", "value")));
        Task task2 = taskWith(Map.of("payload", Map.of("key", "value")));

        String hash1 = (String) worker.execute(task1).getOutputData().get("hash");
        String hash2 = (String) worker.execute(task2).getOutputData().get("hash");

        assertEquals(hash1, hash2);
    }

    @Test
    void differentPayloadsProduceDifferentHashes() {
        Task task1 = taskWith(Map.of("payload", Map.of("key", "value1")));
        Task task2 = taskWith(Map.of("payload", Map.of("key", "value2")));

        String hash1 = (String) worker.execute(task1).getOutputData().get("hash");
        String hash2 = (String) worker.execute(task2).getOutputData().get("hash");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void hashHas16HexChars() {
        Task task = taskWith(Map.of("payload", "test-data"));
        TaskResult result = worker.execute(task);

        String hash = (String) result.getOutputData().get("hash");
        String hexPart = hash.substring("sha256:".length());
        assertEquals(16, hexPart.length());
        assertTrue(hexPart.matches("[0-9a-f]{16}"));
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("payload", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("hash"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("hash"));
    }

    @Test
    void handlesStringPayload() {
        Task task = taskWith(Map.of("payload", "simple-string"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(((String) result.getOutputData().get("hash")).startsWith("sha256:"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
