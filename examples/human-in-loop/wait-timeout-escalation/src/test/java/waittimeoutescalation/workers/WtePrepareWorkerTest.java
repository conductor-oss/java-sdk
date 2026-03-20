package waittimeoutescalation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WtePrepareWorkerTest {

    @Test
    void taskDefName() {
        WtePrepareWorker worker = new WtePrepareWorker();
        assertEquals("wte_prepare", worker.getTaskDefName());
    }

    @Test
    void returnsReadyTrue() {
        WtePrepareWorker worker = new WtePrepareWorker();
        Task task = taskWith(Map.of("requestId", "req-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void completesWithEmptyInput() {
        WtePrepareWorker worker = new WtePrepareWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void outputContainsReadyKey() {
        WtePrepareWorker worker = new WtePrepareWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("ready"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
