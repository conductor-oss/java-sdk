package waittimeoutescalation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WteProcessWorkerTest {

    @Test
    void taskDefName() {
        WteProcessWorker worker = new WteProcessWorker();
        assertEquals("wte_process", worker.getTaskDefName());
    }

    @Test
    void processesResponseCorrectly() {
        WteProcessWorker worker = new WteProcessWorker();
        Task task = taskWith(Map.of("response", "approved"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed-approved", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullResponse() {
        WteProcessWorker worker = new WteProcessWorker();
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed-unknown", result.getOutputData().get("result"));
    }

    @Test
    void processesNumericResponse() {
        WteProcessWorker worker = new WteProcessWorker();
        Task task = taskWith(Map.of("response", 42));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed-42", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsResultKey() {
        WteProcessWorker worker = new WteProcessWorker();
        Task task = taskWith(Map.of("response", "test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
