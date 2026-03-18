package incidentresponse.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotifyOncallWorkerTest {

    private final NotifyOncallWorker worker = new NotifyOncallWorker();

    @Test
    void taskDefName() {
        assertEquals("ir_notify_oncall", worker.getTaskDefName());
    }

    @Test
    void notifiesOncallViaConsoleWhenNoWebhook() {
        Task task = taskWith(Map.of("incidentId", "INC-12345678"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("notified"));
        assertEquals("console", result.getOutputData().get("notificationMethod"));
    }

    @Test
    void outputContainsTimestamp() {
        Task task = taskWith(Map.of("incidentId", "INC-TEST"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("timestamp"));
    }

    @Test
    void outputContainsIncidentId() {
        Task task = taskWith(Map.of("incidentId", "INC-ABCDEF"));
        TaskResult result = worker.execute(task);

        assertEquals("INC-ABCDEF", result.getOutputData().get("incidentId"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("notified"));
    }

    @Test
    void attemptsWebhookWhenUrlProvided() {
        // Use an invalid URL to test the error handling path
        Task task = taskWith(Map.of("incidentId", "INC-TEST", "webhookUrl", "http://localhost:1/nonexistent"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("webhook", result.getOutputData().get("notificationMethod"));
        // Will fail to connect, but should handle gracefully
        assertNotNull(result.getOutputData().get("error"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
