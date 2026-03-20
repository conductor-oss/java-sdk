package datavalidation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckTypesWorkerTest {

    private final CheckTypesWorker worker = new CheckTypesWorker();

    @Test
    void taskDefName() {
        assertEquals("vd_check_types", worker.getTaskDefName());
    }

    @Test
    void correctTypesPasses() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "email", "alice@example.com", "age", 30));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("passedCount"));
        assertEquals(0, result.getOutputData().get("errorCount"));
    }

    @Test
    void nameNotStringFails() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", 123, "email", "test@example.com", "age", 30));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("passedCount"));
        assertEquals(1, result.getOutputData().get("errorCount"));
    }

    @Test
    void ageNotNumberFails() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "email", "test@example.com", "age", "thirty"));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("passedCount"));
        assertEquals(1, result.getOutputData().get("errorCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void bothTypesFail() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", 456, "email", "test@example.com", "age", "old"));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("passedCount"));
        assertEquals(1, result.getOutputData().get("errorCount"));
        List<Map<String, Object>> errors = (List<Map<String, Object>>) result.getOutputData().get("errors");
        List<String> issues = (List<String>) errors.get(0).get("issues");
        assertEquals(2, issues.size());
        assertTrue(issues.contains("name must be string"));
        assertTrue(issues.contains("age must be number"));
    }

    @Test
    void mixedRecordsPassAndFail() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "email", "a@b.com", "age", 30),
                Map.of("name", 999, "email", "c@d.com", "age", 25));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("passedCount"));
        assertEquals(1, result.getOutputData().get("errorCount"));
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

    @Test
    void ageAsDoublePasses() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "email", "a@b.com", "age", 30.5));
        Task task = taskWith(Map.of("records", records, "schema", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("passedCount"));
        assertEquals(0, result.getOutputData().get("errorCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
