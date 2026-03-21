package datavalidation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckRequiredWorkerTest {

    private final CheckRequiredWorker worker = new CheckRequiredWorker();

    @Test
    void taskDefName() {
        assertEquals("vd_check_required", worker.getTaskDefName());
    }

    @Test
    void allFieldsPresentPasses() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "email", "alice@example.com", "age", 30));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("passedCount"));
        assertEquals(0, result.getOutputData().get("errorCount"));
    }

    @Test
    void missingNameFails() {
        Map<String, Object> record = new HashMap<>();
        record.put("name", null);
        record.put("email", "test@example.com");
        record.put("age", 25);
        List<Map<String, Object>> records = List.of(record);
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("passedCount"));
        assertEquals(1, result.getOutputData().get("errorCount"));
    }

    @Test
    void emptyNameFails() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "", "email", "test@example.com", "age", 25));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("passedCount"));
        assertEquals(1, result.getOutputData().get("errorCount"));
    }

    @Test
    void missingEmailFails() {
        Map<String, Object> record = new HashMap<>();
        record.put("name", "Bob");
        record.put("email", null);
        record.put("age", 30);
        List<Map<String, Object>> records = List.of(record);
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("passedCount"));
        assertEquals(1, result.getOutputData().get("errorCount"));
    }

    @Test
    void ageZeroIsValid() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Baby", "email", "baby@example.com", "age", 0));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("passedCount"));
        assertEquals(0, result.getOutputData().get("errorCount"));
    }

    @Test
    void mixedRecordsPassAndFail() {
        Map<String, Object> bad = new HashMap<>();
        bad.put("name", "");
        bad.put("email", "noname@test.com");
        bad.put("age", 40);
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "email", "alice@example.com", "age", 30),
                bad);
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("passedCount"));
        assertEquals(1, result.getOutputData().get("errorCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void errorsContainMissingFieldNames() {
        Map<String, Object> record = new HashMap<>();
        record.put("name", null);
        record.put("email", null);
        record.put("age", 25);
        List<Map<String, Object>> records = List.of(record);
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> errors = (List<Map<String, Object>>) result.getOutputData().get("errors");
        assertEquals(1, errors.size());
        List<String> missing = (List<String>) errors.get(0).get("missing");
        assertTrue(missing.contains("name"));
        assertTrue(missing.contains("email"));
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
