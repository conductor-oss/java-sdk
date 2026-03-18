package batchprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SummarizeWorkerTest {

    private final SummarizeWorker worker = new SummarizeWorker();

    @Test
    void taskDefName() {
        assertEquals("bp_summarize", worker.getTaskDefName());
    }

    @Test
    void generatesSummary() {
        Task task = taskWith(Map.of("totalBatches", 4, "totalRecords", 10, "iterations", 4));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Batch processing complete: 10 records in 4 batches", result.getOutputData().get("summary"));
    }

    @Test
    void handlesSingleBatch() {
        Task task = taskWith(Map.of("totalBatches", 1, "totalRecords", 3, "iterations", 1));
        TaskResult result = worker.execute(task);

        assertEquals("Batch processing complete: 3 records in 1 batches", result.getOutputData().get("summary"));
    }

    @Test
    void handlesZeroRecords() {
        Task task = taskWith(Map.of("totalBatches", 0, "totalRecords", 0, "iterations", 0));
        TaskResult result = worker.execute(task);

        assertEquals("Batch processing complete: 0 records in 0 batches", result.getOutputData().get("summary"));
    }

    @Test
    void handlesDefaultValues() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Batch processing complete: 0 records in 0 batches", result.getOutputData().get("summary"));
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("totalBatches", null);
        input.put("totalRecords", null);
        input.put("iterations", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("summary"));
    }

    @Test
    void handlesLargeNumbers() {
        Task task = taskWith(Map.of("totalBatches", 1000, "totalRecords", 50000, "iterations", 1000));
        TaskResult result = worker.execute(task);

        assertEquals("Batch processing complete: 50000 records in 1000 batches", result.getOutputData().get("summary"));
    }

    @Test
    void summaryContainsRecordAndBatchInfo() {
        Task task = taskWith(Map.of("totalBatches", 5, "totalRecords", 25, "iterations", 5));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("25"));
        assertTrue(summary.contains("5"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
