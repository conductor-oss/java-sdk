package datavalidation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckRangesWorkerTest {

    private final CheckRangesWorker worker = new CheckRangesWorker();

    @Test
    void taskDefName() {
        assertEquals("vd_check_ranges", worker.getTaskDefName());
    }

    @Test
    void validRangesPasses() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "email", "alice@example.com", "age", 30));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("passedCount"));
        assertEquals(0, result.getOutputData().get("errorCount"));
    }

    @Test
    void negativeAgeFails() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Charlie", "email", "charlie@example.com", "age", -5));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("passedCount"));
        assertEquals(1, result.getOutputData().get("errorCount"));
    }

    @Test
    void ageAbove150Fails() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Old", "email", "old@example.com", "age", 200));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("passedCount"));
        assertEquals(1, result.getOutputData().get("errorCount"));
    }

    @Test
    void emailWithoutAtFails() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Bad", "email", "bademail.com", "age", 25));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("passedCount"));
        assertEquals(1, result.getOutputData().get("errorCount"));
    }

    @Test
    void ageBoundaryZeroPasses() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Baby", "email", "baby@example.com", "age", 0));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("passedCount"));
        assertEquals(0, result.getOutputData().get("errorCount"));
    }

    @Test
    void ageBoundary150Passes() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Elder", "email", "elder@example.com", "age", 150));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("passedCount"));
        assertEquals(0, result.getOutputData().get("errorCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void bothAgeAndEmailFail() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Bad", "email", "noemail", "age", -1));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("passedCount"));
        assertEquals(1, result.getOutputData().get("errorCount"));
        List<Map<String, Object>> errors = (List<Map<String, Object>>) result.getOutputData().get("errors");
        List<String> issues = (List<String>) errors.get(0).get("issues");
        assertEquals(2, issues.size());
        assertTrue(issues.contains("age out of range (0-150)"));
        assertTrue(issues.contains("email format invalid"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("schema", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("passedCount"));
        assertEquals(0, result.getOutputData().get("errorCount"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of(), "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("passedCount"));
        assertEquals(0, result.getOutputData().get("errorCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
