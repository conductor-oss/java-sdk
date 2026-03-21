package polltimeout.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PollNormalTaskWorkerTest {

    private final PollNormalTaskWorker worker = new PollNormalTaskWorker();

    @Test
    void taskDefName() {
        assertEquals("poll_normal_task", worker.getTaskDefName());
    }

    @Test
    void processesWithNormalMode() {
        Task task = taskWith(Map.of("mode", "normal"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed", result.getOutputData().get("result"));
    }

    @Test
    void processesWithCustomMode() {
        Task task = taskWith(Map.of("mode", "fast"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed", result.getOutputData().get("result"));
    }

    @Test
    void defaultsModeWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed", result.getOutputData().get("result"));
    }

    @Test
    void defaultsModeWhenNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("mode", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed", result.getOutputData().get("result"));
    }

    @Test
    void defaultsModeWhenBlank() {
        Task task = taskWith(Map.of("mode", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsResultKey() {
        Task task = taskWith(Map.of("mode", "normal"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void resultIsAlwaysProcessed() {
        Task task = taskWith(Map.of("mode", "anything"));
        TaskResult result = worker.execute(task);

        assertEquals("processed", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
