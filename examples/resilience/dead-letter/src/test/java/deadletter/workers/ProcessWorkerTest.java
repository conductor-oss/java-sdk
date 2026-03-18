package deadletter.workers;

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
        assertEquals("dl_process", worker.getTaskDefName());
    }

    @Test
    void succeedsWhenModeIsSuccess() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("mode", "success", "data", "order-123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed: order-123", result.getOutputData().get("result"));
        assertEquals("success", result.getOutputData().get("mode"));
    }

    @Test
    void failsWhenModeIsFail() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("mode", "fail", "data", "order-456"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("Processing failed for data: order-456", result.getOutputData().get("error"));
        assertEquals("fail", result.getOutputData().get("mode"));
    }

    @Test
    void succeedsWhenModeNotProvided() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("data", "order-789"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed: order-789", result.getOutputData().get("result"));
        assertEquals("success", result.getOutputData().get("mode"));
    }

    @Test
    void succeedsWhenNoInputProvided() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed: ", result.getOutputData().get("result"));
        assertEquals("success", result.getOutputData().get("mode"));
    }

    @Test
    void failOutputContainsErrorField() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("mode", "fail", "data", "test-data"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("error"));
        assertFalse(result.getOutputData().containsKey("result"));
    }

    @Test
    void successOutputContainsResultField() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("mode", "success", "data", "test-data"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertFalse(result.getOutputData().containsKey("error"));
    }

    @Test
    void handlesNonStringModeGracefully() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("mode", 123, "data", "order-000"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("mode"));
    }

    @Test
    void handlesNonStringDataGracefully() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("mode", "success", "data", 999));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed: ", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
