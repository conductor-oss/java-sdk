package timeoutpolicies.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CriticalWorkerTest {

    private final CriticalWorker worker = new CriticalWorker();

    @Test
    void taskDefName() {
        assertEquals("tp_critical", worker.getTaskDefName());
    }

    @Test
    void executesSuccessfully() {
        Task task = taskWith(Map.of("mode", "critical"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("critical-done", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsResultKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void resultValueIsCorrect() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        Object value = result.getOutputData().get("result");
        assertInstanceOf(String.class, value);
        assertEquals("critical-done", value);
    }

    @Test
    void statusIsCompleted() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesInputGracefully() {
        Task task = taskWith(Map.of("extraKey", "extraValue"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("critical-done", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
