package eventfiltering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveEventWorkerTest {

    private final ReceiveEventWorker worker = new ReceiveEventWorker();

    @Test
    void taskDefName() {
        assertEquals("ef_receive_event", worker.getTaskDefName());
    }

    @Test
    void receivesEventWithAllFields() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "eventType", "system.alert",
                "severity", "critical",
                "payload", Map.of("service", "payment-gateway")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("system.alert", result.getOutputData().get("eventType"));
        assertEquals("critical", result.getOutputData().get("severity"));
    }

    @Test
    void passesPayloadThrough() {
        Map<String, Object> payload = Map.of("cpu", 98.5, "memory", 92.1);
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "eventType", "metric",
                "severity", "high",
                "payload", payload));
        TaskResult result = worker.execute(task);

        assertEquals(payload, result.getOutputData().get("payload"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void addsMetadataWithSource() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "eventType", "system.alert",
                "severity", "critical",
                "payload", Map.of()));
        TaskResult result = worker.execute(task);

        Map<String, String> metadata = (Map<String, String>) result.getOutputData().get("metadata");
        assertNotNull(metadata);
        assertEquals("monitoring-agent", metadata.get("source"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void addsMetadataWithReceivedAt() {
        Task task = taskWith(Map.of(
                "eventId", "evt-004",
                "eventType", "system.alert",
                "severity", "low",
                "payload", Map.of()));
        TaskResult result = worker.execute(task);

        Map<String, String> metadata = (Map<String, String>) result.getOutputData().get("metadata");
        assertNotNull(metadata);
        assertEquals("2026-01-15T10:00:00Z", metadata.get("receivedAt"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("eventType", "system.alert");
        input.put("severity", "critical");
        input.put("payload", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("system.alert", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullSeverity() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-005");
        input.put("eventType", "system.alert");
        input.put("severity", null);
        input.put("payload", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("severity"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("eventType"));
        assertEquals("", result.getOutputData().get("severity"));
        assertNull(result.getOutputData().get("payload"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
