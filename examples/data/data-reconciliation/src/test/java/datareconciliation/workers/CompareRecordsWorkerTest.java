package datareconciliation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompareRecordsWorkerTest {

    private final CompareRecordsWorker worker = new CompareRecordsWorker();

    @Test
    void taskDefName() {
        assertEquals("rc_compare_records", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void detectsMatchedRecords() {
        List<Map<String, Object>> a = List.of(Map.of("orderId", "O1", "amount", 100));
        List<Map<String, Object>> b = List.of(Map.of("orderId", "O1", "amount", 100));
        Task task = taskWith(Map.of("recordsA", a, "recordsB", b, "keyField", "orderId"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("matchedCount"));
        assertEquals(0, result.getOutputData().get("mismatchedCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void detectsMismatchedRecords() {
        List<Map<String, Object>> a = List.of(Map.of("orderId", "O1", "amount", 100));
        List<Map<String, Object>> b = List.of(Map.of("orderId", "O1", "amount", 200));
        Task task = taskWith(Map.of("recordsA", a, "recordsB", b, "keyField", "orderId"));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("matchedCount"));
        assertEquals(1, result.getOutputData().get("mismatchedCount"));

        List<Map<String, Object>> mismatched = (List<Map<String, Object>>) result.getOutputData().get("mismatched");
        assertEquals("O1", mismatched.get(0).get("key"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void detectsMissingRecords() {
        List<Map<String, Object>> a = List.of(Map.of("orderId", "O1", "amount", 100));
        List<Map<String, Object>> b = List.of(Map.of("orderId", "O2", "amount", 200));
        Task task = taskWith(Map.of("recordsA", a, "recordsB", b, "keyField", "orderId"));
        TaskResult result = worker.execute(task);

        List<String> missingInB = (List<String>) result.getOutputData().get("missingInB");
        List<String> missingInA = (List<String>) result.getOutputData().get("missingInA");
        assertEquals(1, missingInB.size());
        assertEquals(1, missingInA.size());
        assertTrue(missingInB.contains("O1"));
        assertTrue(missingInA.contains("O2"));
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("recordsA", null);
        input.put("recordsB", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("matchedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
