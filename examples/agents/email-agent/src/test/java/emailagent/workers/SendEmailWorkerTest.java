package emailagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SendEmailWorkerTest {

    private final SendEmailWorker worker = new SendEmailWorker();

    @Test
    void taskDefName() {
        assertEquals("ea_send_email", worker.getTaskDefName());
    }

    @Test
    void returnsSentTrue() {
        Task task = taskWith(Map.of(
                "recipient", "sarah@company.com",
                "subject", "Project Alpha Update",
                "body", "Dear Sarah...",
                "approved", "true"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
    }

    @Test
    void returnsFixedMessageId() {
        Task task = taskWith(Map.of("recipient", "sarah@company.com"));
        TaskResult result = worker.execute(task);

        assertEquals("msg-fixed-abc123", result.getOutputData().get("messageId"));
    }

    @Test
    void returnsFixedTimestamp() {
        Task task = taskWith(Map.of("recipient", "sarah@company.com"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-08T10:00:00Z", result.getOutputData().get("timestamp"));
    }

    @Test
    void returnsDeliveredStatus() {
        Task task = taskWith(Map.of("recipient", "sarah@company.com"));
        TaskResult result = worker.execute(task);

        assertEquals("delivered", result.getOutputData().get("deliveryStatus"));
    }

    @Test
    void handlesNullRecipient() {
        Map<String, Object> input = new HashMap<>();
        input.put("recipient", null);
        input.put("subject", "Test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
    }

    @Test
    void handlesBlankBody() {
        Task task = taskWith(Map.of("recipient", "test@test.com", "body", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("messageId"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("deliveryStatus"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
