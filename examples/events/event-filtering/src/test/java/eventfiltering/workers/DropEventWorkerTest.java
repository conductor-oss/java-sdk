package eventfiltering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DropEventWorkerTest {

    private final DropEventWorker worker = new DropEventWorker();

    @Test
    void taskDefName() {
        assertEquals("ef_drop_event", worker.getTaskDefName());
    }

    @Test
    void dropsEventWithReason() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "reason", "Unknown severity level: extreme"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("handled"));
    }

    @Test
    void setsHandlerToDrop() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "reason", "Unknown severity level: none"));
        TaskResult result = worker.execute(task);

        assertEquals("drop", result.getOutputData().get("handler"));
    }

    @Test
    void passesReasonThrough() {
        String reason = "Unknown severity level: foobar";
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "reason", reason));
        TaskResult result = worker.execute(task);

        assertEquals(reason, result.getOutputData().get("reason"));
    }

    @Test
    void setsHandledToFalse() {
        Task task = taskWith(Map.of(
                "eventId", "evt-004",
                "reason", "test reason"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("handled"));
    }

    @Test
    void handlesEmptyReason() {
        Task task = taskWith(Map.of(
                "eventId", "evt-005",
                "reason", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("reason"));
    }

    @Test
    void handlesNullReason() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-006");
        input.put("reason", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("reason"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("handled"));
        assertEquals("drop", result.getOutputData().get("handler"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
