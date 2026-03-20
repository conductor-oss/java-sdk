package waittimeoutescalation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WteEscalateWorkerTest {

    @Test
    void taskDefName() {
        WteEscalateWorker worker = new WteEscalateWorker();
        assertEquals("wte_escalate", worker.getTaskDefName());
    }

    @Test
    void returnsEscalatedTrue() {
        WteEscalateWorker worker = new WteEscalateWorker();
        Task task = taskWith(Map.of("requestId", "req-001", "reason", "timeout"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("escalated"));
    }

    @Test
    void returnsCorrectEscalatedTo() {
        WteEscalateWorker worker = new WteEscalateWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("manager@company.com", result.getOutputData().get("escalatedTo"));
    }

    @Test
    void outputContainsBothKeys() {
        WteEscalateWorker worker = new WteEscalateWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("escalated"));
        assertTrue(result.getOutputData().containsKey("escalatedTo"));
    }

    @Test
    void completesWithEmptyInput() {
        WteEscalateWorker worker = new WteEscalateWorker();
        Task task = taskWith(Map.of());
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
