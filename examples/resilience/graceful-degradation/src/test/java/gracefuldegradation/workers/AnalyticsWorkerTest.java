package gracefuldegradation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsWorkerTest {

    private final AnalyticsWorker worker = new AnalyticsWorker();

    @Test
    void taskDefName() {
        assertEquals("gd_analytics", worker.getTaskDefName());
    }

    @Test
    void availableTrue_returnsTrackedTrue() {
        Task task = taskWith(Map.of("available", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("tracked"));
    }

    @Test
    void availableFalse_returnsTrackedFalse() {
        Task task = taskWith(Map.of("available", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("tracked"));
    }

    @Test
    void noAvailableInput_defaultsToTrue() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("tracked"));
    }

    @Test
    void nullAvailableInput_defaultsToTrue() {
        Map<String, Object> input = new HashMap<>();
        input.put("available", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("tracked"));
    }

    @Test
    void stringTrueIsAccepted() {
        Task task = taskWith(Map.of("available", "true"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("tracked"));
    }

    @Test
    void stringFalseIsAccepted() {
        Task task = taskWith(Map.of("available", "false"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("tracked"));
    }

    @Test
    void outputContainsOnlyTrackedKey() {
        Task task = taskWith(Map.of("available", true));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("tracked"));
        assertEquals(1, result.getOutputData().size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
