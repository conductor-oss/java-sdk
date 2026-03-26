package switchplusfork.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SingleProcessWorkerTest {

    private final SingleProcessWorker worker = new SingleProcessWorker();

    @Test
    void taskDefName() {
        assertEquals("sf_single_process", worker.getTaskDefName());
    }

    @Test
    void returnsModeSingle() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("single", result.getOutputData().get("mode"));
    }

    @Test
    void outputContainsOnlyMode() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("mode"));
    }

    @Test
    void completesWithAnyInput() {
        Task task = taskWith(Map.of("type", "whatever", "items", "something"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("single", result.getOutputData().get("mode"));
    }

    @Test
    void completesWithEmptyInput() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
