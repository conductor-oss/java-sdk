package switchplusfork.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessAWorkerTest {

    private final ProcessAWorker worker = new ProcessAWorker();

    @Test
    void taskDefName() {
        assertEquals("sf_process_a", worker.getTaskDefName());
    }

    @Test
    void returnsLaneAWithItemCount() {
        Task task = taskWith(Map.of("items", List.of("x", "y", "z")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("A", result.getOutputData().get("lane"));
        assertEquals(3, result.getOutputData().get("count"));
    }

    @Test
    void handlesEmptyItems() {
        Task task = taskWith(Map.of("items", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("A", result.getOutputData().get("lane"));
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void handlesSingleItem() {
        Task task = taskWith(Map.of("items", List.of("only")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("count"));
    }

    @Test
    void handlesNullItems() {
        Map<String, Object> input = new HashMap<>();
        input.put("items", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("A", result.getOutputData().get("lane"));
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void handlesMissingItems() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("A", result.getOutputData().get("lane"));
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void outputContainsExpectedFields() {
        Task task = taskWith(Map.of("items", List.of("a", "b")));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("lane"));
        assertTrue(result.getOutputData().containsKey("count"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
