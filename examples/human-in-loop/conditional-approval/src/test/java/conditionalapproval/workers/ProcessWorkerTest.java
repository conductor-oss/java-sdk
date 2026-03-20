package conditionalapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessWorkerTest {

    @Test
    void taskDefName() {
        ProcessWorker worker = new ProcessWorker();
        assertEquals("car_process", worker.getTaskDefName());
    }

    @Test
    void returnsProcessedTrue() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("requestId", "REQ-001", "tier", "low"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void completesWithMediumTier() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("requestId", "REQ-002", "tier", "medium"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void completesWithHighTier() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("requestId", "REQ-003", "tier", "high"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsProcessed() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("requestId", "REQ-004", "tier", "low"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("processed"));
    }

    @Test
    void handlesNullInputsGracefully() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
