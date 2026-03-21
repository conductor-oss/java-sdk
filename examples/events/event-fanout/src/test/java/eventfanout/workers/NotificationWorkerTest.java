package eventfanout.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationWorkerTest {

    private final NotificationWorker worker = new NotificationWorker();

    @Test
    void taskDefName() {
        assertEquals("fo_notification", worker.getTaskDefName());
    }

    @Test
    void notifiesForEvent() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "eventType", "order.created"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("notified", result.getOutputData().get("result"));
        assertEquals("slack", result.getOutputData().get("channel"));
    }

    @Test
    void outputResultIsNotified() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "eventType", "payment.received"));
        TaskResult result = worker.execute(task);

        assertEquals("notified", result.getOutputData().get("result"));
    }

    @Test
    void outputChannelIsSlack() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "eventType", "inventory.low"));
        TaskResult result = worker.execute(task);

        assertEquals("slack", result.getOutputData().get("channel"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("eventType", "order.created");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("notified", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-004");
        input.put("eventType", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("notified", result.getOutputData().get("result"));
        assertEquals("slack", result.getOutputData().get("channel"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("notified", result.getOutputData().get("result"));
        assertEquals("slack", result.getOutputData().get("channel"));
    }

    @Test
    void handlesVariousEventTypes() {
        Task task = taskWith(Map.of(
                "eventId", "evt-005",
                "eventType", "custom.event.type"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("notified", result.getOutputData().get("result"));
        assertEquals("slack", result.getOutputData().get("channel"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
