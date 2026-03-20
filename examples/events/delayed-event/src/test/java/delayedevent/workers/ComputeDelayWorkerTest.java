package delayedevent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComputeDelayWorkerTest {

    private final ComputeDelayWorker worker = new ComputeDelayWorker();

    @Test
    void taskDefName() {
        assertEquals("de_compute_delay", worker.getTaskDefName());
    }

    @Test
    void computesDelayFromSeconds() {
        Task task = taskWith(Map.of(
                "delaySeconds", 30,
                "eventId", "evt-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(30000, result.getOutputData().get("delayMs"));
    }

    @Test
    void outputContainsOriginalDelaySeconds() {
        Task task = taskWith(Map.of(
                "delaySeconds", 10,
                "eventId", "evt-002"));
        TaskResult result = worker.execute(task);

        assertEquals(10, result.getOutputData().get("delaySeconds"));
    }

    @Test
    void handlesStringDelaySeconds() {
        Task task = taskWith(Map.of(
                "delaySeconds", "15",
                "eventId", "evt-003"));
        TaskResult result = worker.execute(task);

        assertEquals(15000, result.getOutputData().get("delayMs"));
    }

    @Test
    void defaultsToFiveSecondsForNullDelay() {
        Map<String, Object> input = new HashMap<>();
        input.put("delaySeconds", null);
        input.put("eventId", "evt-004");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5000, result.getOutputData().get("delayMs"));
    }

    @Test
    void defaultsToFiveSecondsForInvalidDelay() {
        Task task = taskWith(Map.of(
                "delaySeconds", "not-a-number",
                "eventId", "evt-005"));
        TaskResult result = worker.execute(task);

        assertEquals(5000, result.getOutputData().get("delayMs"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5000, result.getOutputData().get("delayMs"));
    }

    @Test
    void computesZeroDelay() {
        Task task = taskWith(Map.of(
                "delaySeconds", 0,
                "eventId", "evt-006"));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("delayMs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
