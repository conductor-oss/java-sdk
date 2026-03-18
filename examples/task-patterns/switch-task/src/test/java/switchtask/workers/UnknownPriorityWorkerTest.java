package switchtask.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UnknownPriorityWorkerTest {

    private final UnknownPriorityWorker worker = new UnknownPriorityWorker();

    @Test
    void taskDefName() {
        assertEquals("sw_unknown_priority", worker.getTaskDefName());
    }

    @Test
    void handlesUnknownPriorityTicket() {
        Task task = taskWith(Map.of("ticketId", "TKT-004", "priority", "CRITICAL", "description", "Unknown severity"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default", result.getOutputData().get("handler"));
        assertEquals(true, result.getOutputData().get("needsClassification"));
    }

    @Test
    void outputContainsExactlyTwoFields() {
        Task task = taskWith(Map.of("ticketId", "TKT-050", "priority", "URGENT", "description", "Test"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("handler"));
        assertTrue(result.getOutputData().containsKey("needsClassification"));
    }

    @Test
    void handlerIsAlwaysDefault() {
        Task task = taskWith(Map.of("ticketId", "TKT-A", "priority", "URGENT", "description", "First"));
        TaskResult result = worker.execute(task);

        assertEquals("default", result.getOutputData().get("handler"));
    }

    @Test
    void needsClassificationIsAlwaysTrue() {
        Task task = taskWith(Map.of("ticketId", "TKT-B", "priority", "NONE", "description", "Second"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("needsClassification"));
    }

    @Test
    void handlesNullTicketId() {
        Map<String, Object> input = new HashMap<>();
        input.put("ticketId", null);
        input.put("priority", "UNKNOWN");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default", result.getOutputData().get("handler"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default", result.getOutputData().get("handler"));
        assertEquals(true, result.getOutputData().get("needsClassification"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
