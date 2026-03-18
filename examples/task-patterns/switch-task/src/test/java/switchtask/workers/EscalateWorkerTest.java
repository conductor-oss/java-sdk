package switchtask.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EscalateWorkerTest {

    private final EscalateWorker worker = new EscalateWorker();

    @Test
    void taskDefName() {
        assertEquals("sw_escalate", worker.getTaskDefName());
    }

    @Test
    void handlesHighPriorityTicket() {
        Task task = taskWith(Map.of("ticketId", "TKT-003", "priority", "HIGH", "description", "Outage"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("manager", result.getOutputData().get("handler"));
        assertEquals("manager@example.com", result.getOutputData().get("escalatedTo"));
    }

    @Test
    void outputContainsExactlyTwoFields() {
        Task task = taskWith(Map.of("ticketId", "TKT-010", "priority", "HIGH", "description", "Test"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("handler"));
        assertTrue(result.getOutputData().containsKey("escalatedTo"));
    }

    @Test
    void escalatedToIsAlwaysManagerEmail() {
        Task task = taskWith(Map.of("ticketId", "TKT-999", "priority", "HIGH", "description", "Critical failure"));
        TaskResult result = worker.execute(task);

        assertEquals("manager@example.com", result.getOutputData().get("escalatedTo"));
    }

    @Test
    void handlerIsAlwaysManager() {
        Task task = taskWith(Map.of("ticketId", "TKT-ABC", "priority", "HIGH", "description", "Any"));
        TaskResult result = worker.execute(task);

        assertEquals("manager", result.getOutputData().get("handler"));
    }

    @Test
    void handlesNullTicketId() {
        Map<String, Object> input = new HashMap<>();
        input.put("ticketId", null);
        input.put("priority", "HIGH");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("manager", result.getOutputData().get("handler"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("manager", result.getOutputData().get("handler"));
        assertEquals("manager@example.com", result.getOutputData().get("escalatedTo"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
