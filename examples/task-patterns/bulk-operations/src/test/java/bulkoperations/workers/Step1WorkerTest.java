package bulkoperations.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Step1WorkerTest {

    private final Step1Worker worker = new Step1Worker();

    @Test
    void taskDefName() {
        assertEquals("bulk_step1", worker.getTaskDefName());
    }

    @Test
    void processesBatchWithId() {
        Task task = taskWith(Map.of("batchId", "42"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("batch-42", result.getOutputData().get("data"));
        assertEquals("42", result.getOutputData().get("batchId"));
    }

    @Test
    void processesBatchWithNumericId() {
        Task task = taskWith(Map.of("batchId", "1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("batch-1", result.getOutputData().get("data"));
        assertEquals("1", result.getOutputData().get("batchId"));
    }

    @Test
    void defaultsBatchIdWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("batch-unknown", result.getOutputData().get("data"));
        assertEquals("unknown", result.getOutputData().get("batchId"));
    }

    @Test
    void defaultsBatchIdWhenBlank() {
        Task task = taskWith(Map.of("batchId", "   "));
        TaskResult result = worker.execute(task);

        assertEquals("batch-unknown", result.getOutputData().get("data"));
        assertEquals("unknown", result.getOutputData().get("batchId"));
    }

    @Test
    void outputContainsDataAndBatchId() {
        Task task = taskWith(Map.of("batchId", "99"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("data"));
        assertTrue(result.getOutputData().containsKey("batchId"));
        assertEquals("batch-99", result.getOutputData().get("data"));
        assertEquals("99", result.getOutputData().get("batchId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
