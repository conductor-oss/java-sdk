package eventfanout.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StorageWorkerTest {

    private final StorageWorker worker = new StorageWorker();

    @Test
    void taskDefName() {
        assertEquals("fo_storage", worker.getTaskDefName());
    }

    @Test
    void storesEventSuccessfully() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "payload", Map.of("orderId", "ORD-100")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("stored", result.getOutputData().get("result"));
        assertEquals("s3://events-lake/evt-001", result.getOutputData().get("storageLocation"));
    }

    @Test
    void outputResultIsStored() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("stored", result.getOutputData().get("result"));
    }

    @Test
    void storageLocationContainsEventId() {
        Task task = taskWith(Map.of(
                "eventId", "evt-abc-123",
                "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("s3://events-lake/evt-abc-123", result.getOutputData().get("storageLocation"));
    }

    @Test
    void storageLocationForDifferentEventId() {
        Task task = taskWith(Map.of(
                "eventId", "evt-xyz-999",
                "payload", Map.of("data", "test")));
        TaskResult result = worker.execute(task);

        assertEquals("s3://events-lake/evt-xyz-999", result.getOutputData().get("storageLocation"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("payload", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("stored", result.getOutputData().get("result"));
        assertEquals("s3://events-lake/unknown", result.getOutputData().get("storageLocation"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("stored", result.getOutputData().get("result"));
        assertEquals("s3://events-lake/unknown", result.getOutputData().get("storageLocation"));
    }

    @Test
    void handlesPayloadWithNestedData() {
        Map<String, Object> payload = Map.of(
                "nested", Map.of("key", "value"),
                "amount", 99.99);
        Task task = taskWith(Map.of(
                "eventId", "evt-nested",
                "payload", payload));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("stored", result.getOutputData().get("result"));
        assertEquals("s3://events-lake/evt-nested", result.getOutputData().get("storageLocation"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
