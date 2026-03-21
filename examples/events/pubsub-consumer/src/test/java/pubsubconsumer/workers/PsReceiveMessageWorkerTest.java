package pubsubconsumer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PsReceiveMessageWorkerTest {

    private final PsReceiveMessageWorker worker = new PsReceiveMessageWorker();

    @Test
    void taskDefName() {
        assertEquals("ps_receive_message", worker.getTaskDefName());
    }

    @Test
    void receivesStandardMessage() {
        Task task = taskWith(Map.of(
                "subscription", "projects/my-project/subscriptions/sensor-readings-sub",
                "messageId", "ps-fixed-001",
                "publishTime", "2026-03-08T10:15:00Z",
                "data", "eyJzZW5zb3JJZCI6...",
                "attributes", Map.of("eventType", "iot.sensor.reading")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("eyJzZW5zb3JJZCI6...", result.getOutputData().get("encodedData"));
        assertEquals("base64", result.getOutputData().get("encoding"));
    }

    @Test
    void passesAttributesThrough() {
        Map<String, Object> attrs = Map.of("eventType", "iot.sensor.reading", "sensorType", "environmental");
        Task task = taskWith(Map.of(
                "subscription", "projects/my-project/subscriptions/test-sub",
                "messageId", "msg-002",
                "publishTime", "2026-03-08T10:15:00Z",
                "data", "abc123",
                "attributes", attrs));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> outputAttrs = (Map<String, Object>) result.getOutputData().get("attributes");
        assertNotNull(outputAttrs);
        assertEquals("iot.sensor.reading", outputAttrs.get("eventType"));
        assertEquals("environmental", outputAttrs.get("sensorType"));
    }

    @Test
    void setsReceivedAtTimestamp() {
        Task task = taskWith(Map.of(
                "subscription", "projects/my-project/subscriptions/test-sub",
                "messageId", "msg-003",
                "publishTime", "2026-03-08T10:15:00Z",
                "data", "somedata",
                "attributes", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("receivedAt"));
    }

    @Test
    void handlesEmptyData() {
        Map<String, Object> input = new HashMap<>();
        input.put("subscription", "projects/my-project/subscriptions/test-sub");
        input.put("messageId", "msg-004");
        input.put("publishTime", "2026-03-08T10:15:00Z");
        input.put("data", "");
        input.put("attributes", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("encodedData"));
    }

    @Test
    void handlesMissingAttributes() {
        Task task = taskWith(Map.of(
                "subscription", "projects/my-project/subscriptions/test-sub",
                "messageId", "msg-005",
                "publishTime", "2026-03-08T10:15:00Z",
                "data", "payload"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("attributes"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("base64", result.getOutputData().get("encoding"));
        assertNotNull(result.getOutputData().get("receivedAt"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("subscription", "projects/test/subscriptions/sub");
        input.put("messageId", "msg-006");
        input.put("data", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("encodedData"));
    }

    @Test
    void handlesNullSubscription() {
        Map<String, Object> input = new HashMap<>();
        input.put("subscription", null);
        input.put("messageId", "msg-007");
        input.put("data", "payload");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("base64", result.getOutputData().get("encoding"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
