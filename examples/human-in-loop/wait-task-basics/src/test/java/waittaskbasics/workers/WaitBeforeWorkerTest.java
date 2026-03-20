package waittaskbasics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WaitBeforeWorkerTest {

    @Test
    void taskDefName() {
        WaitBeforeWorker worker = new WaitBeforeWorker();
        assertEquals("wait_before", worker.getTaskDefName());
    }

    @Test
    void returnsPreparedTrue() {
        WaitBeforeWorker worker = new WaitBeforeWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("prepared"));
    }

    @Test
    void completesWithAnyInput() {
        WaitBeforeWorker worker = new WaitBeforeWorker();
        Task task = taskWith(Map.of("requestId", "req-abc"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("prepared"));
    }

    @Test
    void outputContainsPreparedKey() {
        WaitBeforeWorker worker = new WaitBeforeWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("prepared"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
