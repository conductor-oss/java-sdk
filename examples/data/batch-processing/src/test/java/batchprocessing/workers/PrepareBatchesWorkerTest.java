package batchprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrepareBatchesWorkerTest {

    private final PrepareBatchesWorker worker = new PrepareBatchesWorker();

    @Test
    void taskDefName() {
        assertEquals("bp_prepare_batches", worker.getTaskDefName());
    }

    @Test
    void preparesCorrectBatchCount() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1), Map.of("id", 2), Map.of("id", 3),
                        Map.of("id", 4), Map.of("id", 5), Map.of("id", 6),
                        Map.of("id", 7), Map.of("id", 8), Map.of("id", 9), Map.of("id", 10)),
                "batchSize", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(10, result.getOutputData().get("totalRecords"));
        assertEquals(4, result.getOutputData().get("totalBatches"));
        assertEquals(3, result.getOutputData().get("batchSize"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void createsBatchChunks() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1), Map.of("id", 2), Map.of("id", 3),
                        Map.of("id", 4), Map.of("id", 5)),
                "batchSize", 2));
        TaskResult result = worker.execute(task);

        List<List<Object>> batches = (List<List<Object>>) result.getOutputData().get("batches");
        assertNotNull(batches);
        assertEquals(3, batches.size());
        assertEquals(2, batches.get(0).size());
        assertEquals(2, batches.get(1).size());
        assertEquals(1, batches.get(2).size());
    }

    @Test
    void handlesExactDivisibleRecords() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1), Map.of("id", 2), Map.of("id", 3),
                        Map.of("id", 4), Map.of("id", 5), Map.of("id", 6)),
                "batchSize", 3));
        TaskResult result = worker.execute(task);

        assertEquals(6, result.getOutputData().get("totalRecords"));
        assertEquals(2, result.getOutputData().get("totalBatches"));
    }

    @Test
    void handlesSingleRecord() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1)),
                "batchSize", 5));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("totalRecords"));
        assertEquals(1, result.getOutputData().get("totalBatches"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("batchSize", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalRecords"));
        assertEquals(0, result.getOutputData().get("totalBatches"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("batchSize", 3);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalRecords"));
    }

    @Test
    void defaultsBatchSizeToThree() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1), Map.of("id", 2), Map.of("id", 3),
                        Map.of("id", 4), Map.of("id", 5))));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("batchSize"));
        assertEquals(2, result.getOutputData().get("totalBatches"));
    }

    @Test
    void handlesLargeBatchSize() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1), Map.of("id", 2)),
                "batchSize", 100));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("totalRecords"));
        assertEquals(1, result.getOutputData().get("totalBatches"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void batchesContainOriginalData() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("name", "Alice"), Map.of("name", "Bob"), Map.of("name", "Charlie")),
                "batchSize", 2));
        TaskResult result = worker.execute(task);

        List<List<Object>> batches = (List<List<Object>>) result.getOutputData().get("batches");
        Map<String, Object> firstItem = (Map<String, Object>) batches.get(0).get(0);
        assertEquals("Alice", firstItem.get("name"));
        Map<String, Object> lastItem = (Map<String, Object>) batches.get(1).get(0);
        assertEquals("Charlie", lastItem.get("name"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
