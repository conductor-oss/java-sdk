package workflowtimeout.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FastWorkerTest {

    private final FastWorker worker = new FastWorker();

    @Test
    void taskDefName() {
        assertEquals("wft_fast", worker.getTaskDefName());
    }

    @Test
    void completesWithMode() {
        Task task = taskWith(Map.of("mode", "quick"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("done-quick", result.getOutputData().get("result"));
    }

    @Test
    void completesWithDifferentMode() {
        Task task = taskWith(Map.of("mode", "extended"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("done-extended", result.getOutputData().get("result"));
    }

    @Test
    void defaultsModeWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("done-default", result.getOutputData().get("result"));
    }

    @Test
    void defaultsModeWhenBlank() {
        Task task = taskWith(Map.of("mode", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("done-default", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsResultKey() {
        Task task = taskWith(Map.of("mode", "test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertEquals(1, result.getOutputData().size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
