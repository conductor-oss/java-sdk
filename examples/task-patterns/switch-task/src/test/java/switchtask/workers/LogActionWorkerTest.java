package switchtask.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogActionWorkerTest {

    private final LogActionWorker worker = new LogActionWorker();

    @Test
    void taskDefName() {
        assertEquals("sw_log_action", worker.getTaskDefName());
    }

    @Test
    void logsTicketAction() {
        Task task = taskWith(Map.of("ticketId", "TKT-001", "priority", "LOW"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void outputContainsExactlyOneField() {
        Task task = taskWith(Map.of("ticketId", "TKT-010", "priority", "MEDIUM"));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("logged"));
    }

    @Test
    void loggedIsAlwaysTrue() {
        Task task = taskWith(Map.of("ticketId", "TKT-A", "priority", "HIGH"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void worksWithAnyPriority() {
        for (String priority : new String[]{"LOW", "MEDIUM", "HIGH", "CRITICAL", "UNKNOWN"}) {
            Task task = taskWith(Map.of("ticketId", "TKT-X", "priority", priority));
            TaskResult result = worker.execute(task);

            assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
            assertEquals(true, result.getOutputData().get("logged"),
                    "Should log for priority: " + priority);
        }
    }

    @Test
    void handlesNullTicketId() {
        Map<String, Object> input = new HashMap<>();
        input.put("ticketId", null);
        input.put("priority", "LOW");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
