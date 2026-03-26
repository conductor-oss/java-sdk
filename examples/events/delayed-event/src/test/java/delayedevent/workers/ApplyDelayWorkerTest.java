package delayedevent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplyDelayWorkerTest {

    private final ApplyDelayWorker worker = new ApplyDelayWorker();

    @Test
    void taskDefName() {
        assertEquals("de_apply_delay", worker.getTaskDefName());
    }

    @Test
    void appliesDelaySuccessfully() {
        Task task = taskWith(Map.of(
                "delayMs", 30000,
                "eventId", "evt-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("delayed"));
        assertEquals(30000, result.getOutputData().get("actualDelayMs"));
    }

    @Test
    void outputContainsDelayedFlag() {
        Task task = taskWith(Map.of(
                "delayMs", 5000,
                "eventId", "evt-002"));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("delayed"));
    }

    @Test
    void actualDelayMsMatchesInput() {
        Task task = taskWith(Map.of(
                "delayMs", 10000,
                "eventId", "evt-003"));
        TaskResult result = worker.execute(task);

        assertEquals(10000, result.getOutputData().get("actualDelayMs"));
    }

    @Test
    void handlesZeroDelay() {
        Task task = taskWith(Map.of(
                "delayMs", 0,
                "eventId", "evt-004"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("delayed"));
        assertEquals(0, result.getOutputData().get("actualDelayMs"));
    }

    @Test
    void handlesNullDelayMs() {
        Map<String, Object> input = new HashMap<>();
        input.put("delayMs", null);
        input.put("eventId", "evt-005");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("delayed"));
        assertNull(result.getOutputData().get("actualDelayMs"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("delayed"));
    }

    @Test
    void handlesLargeDelay() {
        Task task = taskWith(Map.of(
                "delayMs", 3600000,
                "eventId", "evt-006"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3600000, result.getOutputData().get("actualDelayMs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
