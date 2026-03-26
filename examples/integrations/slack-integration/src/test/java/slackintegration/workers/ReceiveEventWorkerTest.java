package slackintegration.workers;

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
        assertEquals("slk_receive_event", worker.getTaskDefName());
    }

    @Test
    void receivesDeploymentEvent() {
        Task task = taskWith(Map.of("channel", "engineering", "eventType", "deployment",
                "payload", Map.of("service", "api-gateway")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("deployment", result.getOutputData().get("eventType"));
        assertNotNull(result.getOutputData().get("data"));
        assertNotNull(result.getOutputData().get("receivedAt"));
    }

    @Test
    void receivesAlertEvent() {
        Task task = taskWith(Map.of("channel", "alerts", "eventType", "alert",
                "payload", Map.of("severity", "high")));
        TaskResult result = worker.execute(task);

        assertEquals("alert", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("channel", "general");
        input.put("eventType", null);
        input.put("payload", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullChannel() {
        Map<String, Object> input = new HashMap<>();
        input.put("channel", null);
        input.put("eventType", "test");
        input.put("payload", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventType"));
    }

    @Test
    void passesPayloadThroughAsData() {
        Map<String, Object> payload = Map.of("key1", "value1", "key2", 42);
        Task task = taskWith(Map.of("channel", "test", "eventType", "custom", "payload", payload));
        TaskResult result = worker.execute(task);

        assertEquals(payload, result.getOutputData().get("data"));
    }

    @Test
    void outputContainsReceivedAt() {
        Task task = taskWith(Map.of("channel", "dev", "eventType", "build", "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("receivedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
