package batchprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessBatchWorkerTest {

    private final ProcessBatchWorker worker = new ProcessBatchWorker();

    @Test
    void taskDefName() {
        assertEquals("bp_process_batch", worker.getTaskDefName());
    }

    @Test
    void processesFirstBatch() {
        List<List<Object>> batches = List.of(
                List.of(Map.of("id", 1, "name", "Alice"), Map.of("id", 2, "name", "Bob"), Map.of("id", 3, "name", "Charlie")),
                List.of(Map.of("id", 4, "name", "Diana")));
        Task task = taskWith(Map.of("iteration", 0, "batchSize", 3, "totalRecords", 4, "batches", batches));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("batchIndex"));
        assertEquals(3, result.getOutputData().get("processedCount"));
        assertEquals(0, result.getOutputData().get("rangeStart"));
        assertEquals(3, result.getOutputData().get("rangeEnd"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void processesAndNormalizesRecords() {
        List<List<Object>> batches = List.of(
                List.of(Map.of("id", 1, "name", "  Alice  Johnson  ", "email", "ALICE@CORP.COM")));
        Task task = taskWith(Map.of("iteration", 0, "batchSize", 3, "totalRecords", 1, "batches", batches));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> items = (List<Map<String, Object>>) result.getOutputData().get("processedItems");
        assertEquals(1, items.size());
        assertEquals("Alice Johnson", items.get(0).get("name"));
        assertEquals("ALICE@CORP.COM", items.get(0).get("email"));
        assertTrue((Boolean) items.get(0).get("_valid"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void marksEmptyRecordsAsInvalid() {
        Map<String, Object> emptyRecord = new HashMap<>();
        emptyRecord.put("name", "");
        emptyRecord.put("value", "");
        List<List<Object>> batches = List.of(List.of(emptyRecord));
        Task task = taskWith(Map.of("iteration", 0, "batchSize", 3, "totalRecords", 1, "batches", batches));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> items = (List<Map<String, Object>>) result.getOutputData().get("processedItems");
        assertFalse((Boolean) items.get(0).get("_valid"));
    }

    @Test
    void processesMiddleBatch() {
        List<List<Object>> batches = List.of(
                List.of(Map.of("id", 1), Map.of("id", 2), Map.of("id", 3)),
                List.of(Map.of("id", 4), Map.of("id", 5), Map.of("id", 6)));
        Task task = taskWith(Map.of("iteration", 1, "batchSize", 3, "totalRecords", 6, "batches", batches));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("batchIndex"));
        assertEquals(3, result.getOutputData().get("processedCount"));
        assertEquals(3, result.getOutputData().get("rangeStart"));
        assertEquals(6, result.getOutputData().get("rangeEnd"));
    }

    @Test
    void processesLastPartialBatch() {
        List<List<Object>> batches = List.of(
                List.of(Map.of("id", 1), Map.of("id", 2), Map.of("id", 3)),
                List.of(Map.of("id", 4), Map.of("id", 5), Map.of("id", 6)),
                List.of(Map.of("id", 7), Map.of("id", 8), Map.of("id", 9)),
                List.of(Map.of("id", 10)));
        Task task = taskWith(Map.of("iteration", 3, "batchSize", 3, "totalRecords", 10, "batches", batches));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("batchIndex"));
        assertEquals(1, result.getOutputData().get("processedCount"));
        assertEquals(9, result.getOutputData().get("rangeStart"));
        assertEquals(10, result.getOutputData().get("rangeEnd"));
    }

    @Test
    void handlesDefaultValues() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("batchIndex"));
        assertEquals(0, result.getOutputData().get("processedCount"));
    }

    @Test
    void processesLargeBatch() {
        Task task = taskWith(Map.of("iteration", 0, "batchSize", 100, "totalRecords", 50));
        TaskResult result = worker.execute(task);

        assertEquals(50, result.getOutputData().get("processedCount"));
        assertEquals(0, result.getOutputData().get("rangeStart"));
        assertEquals(50, result.getOutputData().get("rangeEnd"));
    }

    @Test
    void handlesExactBatchBoundary() {
        Task task = taskWith(Map.of("iteration", 1, "batchSize", 5, "totalRecords", 10));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("processedCount"));
        assertEquals(5, result.getOutputData().get("rangeStart"));
        assertEquals(10, result.getOutputData().get("rangeEnd"));
    }

    @Test
    void handlesSingleRecordBatch() {
        Task task = taskWith(Map.of("iteration", 0, "batchSize", 1, "totalRecords", 1));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("processedCount"));
        assertEquals(0, result.getOutputData().get("rangeStart"));
        assertEquals(1, result.getOutputData().get("rangeEnd"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void tracksOriginalIndex() {
        List<List<Object>> batches = List.of(
                List.of(Map.of("id", 1), Map.of("id", 2)),
                List.of(Map.of("id", 3), Map.of("id", 4)));
        Task task = taskWith(Map.of("iteration", 1, "batchSize", 2, "totalRecords", 4, "batches", batches));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> items = (List<Map<String, Object>>) result.getOutputData().get("processedItems");
        assertEquals(2, items.size());
        assertEquals(2, items.get(0).get("originalIndex"));
        assertEquals(3, items.get(1).get("originalIndex"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void countsFieldsPerRecord() {
        List<List<Object>> batches = List.of(
                List.of(Map.of("id", 1, "name", "Alice", "email", "alice@corp.com")));
        Task task = taskWith(Map.of("iteration", 0, "batchSize", 3, "totalRecords", 1, "batches", batches));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> items = (List<Map<String, Object>>) result.getOutputData().get("processedItems");
        assertEquals(3, items.get(0).get("_fieldCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
