package responsetimeout.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RespTimeoutWorkerTest {

    private RespTimeoutWorker worker;

    @BeforeEach
    void setUp() {
        worker = new RespTimeoutWorker();
    }

    @Test
    void taskDefName() {
        assertEquals("resp_timeout_task", worker.getTaskDefName());
    }

    @Test
    void completesWithFastResponse() {
        Task task = taskWith(Map.of("mode", "fast"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("fast-response", result.getOutputData().get("result"));
    }

    @Test
    void returnsAttemptNumber() {
        Task task = taskWith(Map.of("mode", "fast"));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("attempt"));
    }

    @Test
    void incrementsAttemptOnSubsequentCalls() {
        Task task1 = taskWith(Map.of("mode", "fast"));
        worker.execute(task1);

        Task task2 = taskWith(Map.of("mode", "fast"));
        TaskResult result2 = worker.execute(task2);

        assertEquals(2, result2.getOutputData().get("attempt"));
    }

    @Test
    void defaultsModeWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("fast-response", result.getOutputData().get("result"));
    }

    @Test
    void defaultsModeWhenBlank() {
        Task task = taskWith(Map.of("mode", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("fast-response", result.getOutputData().get("result"));
    }

    @Test
    void resetAttemptCount() {
        Task task = taskWith(Map.of("mode", "fast"));
        worker.execute(task);
        assertEquals(1, worker.getAttemptCount());

        worker.resetAttemptCount();
        assertEquals(0, worker.getAttemptCount());

        TaskResult result = worker.execute(task);
        assertEquals(1, result.getOutputData().get("attempt"));
    }

    @Test
    void outputContainsBothFields() {
        Task task = taskWith(Map.of("mode", "fast"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("attempt"));
        assertEquals(2, result.getOutputData().size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
