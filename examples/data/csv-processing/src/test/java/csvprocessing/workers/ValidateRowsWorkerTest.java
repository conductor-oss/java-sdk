package csvprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateRowsWorkerTest {

    private final ValidateRowsWorker worker = new ValidateRowsWorker();

    @Test
    void taskDefName() {
        assertEquals("cv_validate_rows", worker.getTaskDefName());
    }

    @Test
    void validatesRowsWithNameAndEmail() {
        Task task = taskWith(Map.of(
                "rows", List.of(
                        Map.of("name", "Alice", "email", "alice@corp.com"),
                        Map.of("name", "Bob", "email", "bob@corp.com")),
                "headers", List.of("name", "email")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("validCount"));
        assertEquals(0, result.getOutputData().get("invalidCount"));
    }

    @Test
    void rejectsMissingName() {
        Task task = taskWith(Map.of(
                "rows", List.of(
                        Map.of("name", "", "email", "nobody@corp.com")),
                "headers", List.of("name", "email")));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("validCount"));
        assertEquals(1, result.getOutputData().get("invalidCount"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List<Map<String, Object>>) result.getOutputData().get("errors");
        assertEquals("missing name", errors.get(0).get("reason"));
    }

    @Test
    void rejectsInvalidEmail() {
        Task task = taskWith(Map.of(
                "rows", List.of(
                        Map.of("name", "Charlie", "email", "not-an-email")),
                "headers", List.of("name", "email")));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("validCount"));
        assertEquals(1, result.getOutputData().get("invalidCount"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List<Map<String, Object>>) result.getOutputData().get("errors");
        assertEquals("invalid email", errors.get(0).get("reason"));
    }

    @Test
    void mixedValidAndInvalidRows() {
        Task task = taskWith(Map.of(
                "rows", List.of(
                        Map.of("name", "Alice", "email", "alice@corp.com"),
                        Map.of("name", "", "email", "bad-email"),
                        Map.of("name", "Diana", "email", "diana@corp.com")),
                "headers", List.of("name", "email")));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("validCount"));
        assertEquals(1, result.getOutputData().get("invalidCount"));
    }

    @Test
    void handlesNullRows() {
        Map<String, Object> input = new HashMap<>();
        input.put("rows", null);
        input.put("headers", List.of("name", "email"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("validCount"));
        assertEquals(0, result.getOutputData().get("invalidCount"));
    }

    @Test
    void handlesEmptyRows() {
        Task task = taskWith(Map.of(
                "rows", List.of(),
                "headers", List.of("name", "email")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("validCount"));
        assertEquals(0, result.getOutputData().get("invalidCount"));
    }

    @Test
    void errorsIncludeRowNumber() {
        Task task = taskWith(Map.of(
                "rows", List.of(
                        Map.of("name", "Alice", "email", "alice@corp.com"),
                        Map.of("name", "", "email", "bad"),
                        Map.of("name", "Charlie", "email", "no-at-sign")),
                "headers", List.of("name", "email")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List<Map<String, Object>>) result.getOutputData().get("errors");
        assertEquals(2, errors.size());
        assertEquals(2, errors.get(0).get("row"));
        assertEquals(3, errors.get(1).get("row"));
    }

    @Test
    void validRowsContainOriginalData() {
        Map<String, Object> row = new HashMap<>();
        row.put("name", "Alice");
        row.put("email", "alice@corp.com");
        row.put("dept", "engineering");
        row.put("salary", "95000");
        Task task = taskWith(Map.of(
                "rows", List.of(row),
                "headers", List.of("name", "email", "dept", "salary")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> validRows = (List<Map<String, Object>>) result.getOutputData().get("validRows");
        assertEquals(1, validRows.size());
        assertEquals("Alice", validRows.get(0).get("name"));
        assertEquals("engineering", validRows.get(0).get("dept"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
