package switchtask.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AutoHandleWorkerTest {

    private final AutoHandleWorker worker = new AutoHandleWorker();

    @Test
    void taskDefName() {
        assertEquals("sw_auto_handle", worker.getTaskDefName());
    }

    @Test
    void handlesLowPriorityTicket() {
        Task task = taskWith(Map.of("ticketId", "TKT-001", "priority", "LOW", "description", "Minor issue"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("auto", result.getOutputData().get("handler"));
        assertEquals(true, result.getOutputData().get("resolved"));
    }

    @Test
    void outputContainsExactlyTwoFields() {
        Task task = taskWith(Map.of("ticketId", "TKT-010", "priority", "LOW", "description", "Test"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("handler"));
        assertTrue(result.getOutputData().containsKey("resolved"));
    }

    @Test
    void handlerIsAlwaysAuto() {
        Task task = taskWith(Map.of("ticketId", "TKT-ABC", "priority", "LOW", "description", "Any description"));
        TaskResult result = worker.execute(task);

        assertEquals("auto", result.getOutputData().get("handler"));
    }

    @Test
    void resolvedIsAlwaysTrue() {
        Task task = taskWith(Map.of("ticketId", "TKT-XYZ", "priority", "LOW", "description", "Another one"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("resolved"));
    }

    @Test
    void handlesNullTicketId() {
        Map<String, Object> input = new HashMap<>();
        input.put("ticketId", null);
        input.put("priority", "LOW");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("auto", result.getOutputData().get("handler"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("auto", result.getOutputData().get("handler"));
        assertEquals(true, result.getOutputData().get("resolved"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
