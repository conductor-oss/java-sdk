package eventwindowing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmitResultWorkerTest {

    private final EmitResultWorker worker = new EmitResultWorker();

    @Test
    void taskDefName() {
        assertEquals("ew_emit_result", worker.getTaskDefName());
    }

    @Test
    void emitsResultSuccessfully() {
        Map<String, Object> stats = Map.of(
                "count", 5, "min", 10, "max", 30, "sum", 100, "avg", "20.00");
        Task task = taskWith(Map.of("windowId", "win_fixed_001", "stats", stats));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("emitted"));
    }

    @Test
    void returnsWindowId() {
        Map<String, Object> stats = Map.of("count", 5);
        Task task = taskWith(Map.of("windowId", "win_fixed_001", "stats", stats));
        TaskResult result = worker.execute(task);

        assertEquals("win_fixed_001", result.getOutputData().get("windowId"));
    }

    @Test
    void emittedIsTrue() {
        Task task = taskWith(Map.of("windowId", "win_fixed_001"));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("emitted"));
    }

    @Test
    void handlesNullWindowId() {
        Map<String, Object> input = new HashMap<>();
        input.put("windowId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("win_fixed_001", result.getOutputData().get("windowId"));
    }

    @Test
    void handlesEmptyWindowId() {
        Task task = taskWith(Map.of("windowId", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("win_fixed_001", result.getOutputData().get("windowId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("emitted"));
        assertEquals("win_fixed_001", result.getOutputData().get("windowId"));
    }

    @Test
    void outputContainsOnlyEmittedAndWindowId() {
        Task task = taskWith(Map.of("windowId", "win_fixed_001"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("emitted"));
        assertTrue(result.getOutputData().containsKey("windowId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
