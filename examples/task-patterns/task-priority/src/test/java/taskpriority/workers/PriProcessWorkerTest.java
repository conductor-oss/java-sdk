package taskpriority.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PriProcessWorkerTest {

    private final PriProcessWorker worker = new PriProcessWorker();

    @Test
    void taskDefName() {
        assertEquals("pri_process", worker.getTaskDefName());
    }

    @Test
    void processesRequestWithPriority() {
        Task task = taskWith(Map.of("requestId", "req-123", "priority", 50));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("req-123", result.getOutputData().get("requestId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void processesHighPriority() {
        Task task = taskWith(Map.of("requestId", "req-critical", "priority", 99));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("req-critical", result.getOutputData().get("requestId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void processesZeroPriority() {
        Task task = taskWith(Map.of("requestId", "req-default", "priority", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("req-default", result.getOutputData().get("requestId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void defaultsRequestIdWhenMissing() {
        Task task = taskWith(Map.of("priority", 10));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("requestId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void defaultsRequestIdWhenBlank() {
        Task task = taskWith(Map.of("requestId", "   ", "priority", 10));
        TaskResult result = worker.execute(task);

        assertEquals("unknown", result.getOutputData().get("requestId"));
    }

    @Test
    void handlesPriorityAsString() {
        Task task = taskWith(Map.of("requestId", "req-str", "priority", "75"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("req-str", result.getOutputData().get("requestId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesInvalidPriorityString() {
        Task task = taskWith(Map.of("requestId", "req-bad", "priority", "not-a-number"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("req-bad", result.getOutputData().get("requestId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesMissingPriority() {
        Task task = taskWith(Map.of("requestId", "req-nopri"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("req-nopri", result.getOutputData().get("requestId"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
