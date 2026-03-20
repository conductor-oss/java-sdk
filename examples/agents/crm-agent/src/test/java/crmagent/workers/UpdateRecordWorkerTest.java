package crmagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdateRecordWorkerTest {

    private final UpdateRecordWorker worker = new UpdateRecordWorker();

    @Test
    void taskDefName() {
        assertEquals("cm_update_record", worker.getTaskDefName());
    }

    @Test
    void updatesRecordSuccessfully() {
        Task task = taskWith(Map.of("customerId", "CUST-4821",
                "inquiry", "API timeout errors",
                "channel", "email",
                "interactionCount", 47));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TKT-FIXED-001", result.getOutputData().get("ticketId"));
        assertEquals(true, result.getOutputData().get("recordUpdated"));
        assertEquals(48, result.getOutputData().get("newInteractionCount"));
        assertEquals("2026-03-08T10:00:00Z", result.getOutputData().get("timestamp"));
    }

    @Test
    void incrementsInteractionCount() {
        Task task = taskWith(Map.of("customerId", "CUST-4821",
                "inquiry", "Test", "channel", "phone", "interactionCount", 10));
        TaskResult result = worker.execute(task);

        assertEquals(11, result.getOutputData().get("newInteractionCount"));
    }

    @Test
    void handlesZeroInteractionCount() {
        Task task = taskWith(Map.of("customerId", "CUST-4821",
                "inquiry", "Test", "channel", "chat", "interactionCount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("newInteractionCount"));
    }

    @Test
    void handlesMissingInteractionCount() {
        Task task = taskWith(Map.of("customerId", "CUST-4821",
                "inquiry", "Test", "channel", "email"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("newInteractionCount"));
    }

    @Test
    void handlesNullCustomerId() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", null);
        input.put("inquiry", "Test");
        input.put("channel", "email");
        input.put("interactionCount", 5);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TKT-FIXED-001", result.getOutputData().get("ticketId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TKT-FIXED-001", result.getOutputData().get("ticketId"));
        assertEquals(true, result.getOutputData().get("recordUpdated"));
        assertEquals("2026-03-08T10:00:00Z", result.getOutputData().get("timestamp"));
    }

    @Test
    void usesFixedTicketIdAndTimestamp() {
        Task task = taskWith(Map.of("customerId", "CUST-9999",
                "inquiry", "Different inquiry", "channel", "chat", "interactionCount", 100));
        TaskResult result = worker.execute(task);

        assertEquals("TKT-FIXED-001", result.getOutputData().get("ticketId"));
        assertEquals("2026-03-08T10:00:00Z", result.getOutputData().get("timestamp"));
    }

    @Test
    void handlesBlankInquiry() {
        Task task = taskWith(Map.of("customerId", "CUST-4821",
                "inquiry", "  ", "channel", "email", "interactionCount", 47));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recordUpdated"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
