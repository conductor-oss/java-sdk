package pubsubconsumer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PsAckMessageWorkerTest {

    private final PsAckMessageWorker worker = new PsAckMessageWorker();

    @Test
    void taskDefName() {
        assertEquals("ps_ack_message", worker.getTaskDefName());
    }

    @Test
    void acknowledgesStandardMessage() {
        Task task = taskWith(Map.of(
                "subscription", "projects/my-project/subscriptions/sensor-readings-sub",
                "messageId", "ps-fixed-001",
                "processingResult", Map.of("processed", true, "result", "stored")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void setsAckedAtTimestamp() {
        Task task = taskWith(Map.of(
                "subscription", "projects/my-project/subscriptions/test-sub",
                "messageId", "msg-002",
                "processingResult", Map.of("processed", true)));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("ackedAt"));
    }

    @Test
    void handlesProcessingResultWithAlerts() {
        Task task = taskWith(Map.of(
                "subscription", "projects/my-project/subscriptions/test-sub",
                "messageId", "msg-003",
                "processingResult", Map.of("processed", true, "alertCount", 2)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void handlesMissingProcessingResult() {
        Task task = taskWith(Map.of(
                "subscription", "projects/my-project/subscriptions/test-sub",
                "messageId", "msg-004"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("acknowledged"));
        assertNotNull(result.getOutputData().get("ackedAt"));
    }

    @Test
    void handlesNullSubscription() {
        Map<String, Object> input = new HashMap<>();
        input.put("subscription", null);
        input.put("messageId", "msg-005");
        input.put("processingResult", Map.of("processed", true));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void handlesNullMessageId() {
        Map<String, Object> input = new HashMap<>();
        input.put("subscription", "projects/test/subscriptions/sub");
        input.put("messageId", null);
        input.put("processingResult", Map.of("processed", true));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void handlesNullProcessingResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("subscription", "projects/test/subscriptions/sub");
        input.put("messageId", "msg-006");
        input.put("processingResult", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
