package etlbasics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateOutputWorkerTest {

    private final ValidateOutputWorker worker = new ValidateOutputWorker();

    @Test
    void taskDefName() {
        assertEquals("el_validate_output", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void acceptsValidRecords() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@example.com", "amount", 150.50)
        );
        Task task = taskWith(Map.of("transformedRecords", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> validRecords =
                (List<Map<String, Object>>) result.getOutputData().get("validRecords");
        assertEquals(1, validRecords.size());
        assertEquals(1, result.getOutputData().get("validCount"));
        assertEquals(0, result.getOutputData().get("invalidCount"));
    }

    @Test
    void rejectsRecordWithEmptyName() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "", "email", "test@test.com", "amount", 100.0)
        );
        Task task = taskWith(Map.of("transformedRecords", records));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("validCount"));
        assertEquals(1, result.getOutputData().get("invalidCount"));
    }

    @Test
    void rejectsRecordWithEmptyEmail() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Test", "email", "", "amount", 100.0)
        );
        Task task = taskWith(Map.of("transformedRecords", records));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("validCount"));
        assertEquals(1, result.getOutputData().get("invalidCount"));
    }

    @Test
    void rejectsRecordWithZeroAmount() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Test", "email", "test@test.com", "amount", 0.0)
        );
        Task task = taskWith(Map.of("transformedRecords", records));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("validCount"));
        assertEquals(1, result.getOutputData().get("invalidCount"));
    }

    @Test
    void filtersValidAndInvalidRecords() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@example.com", "amount", 150.50),
                Map.of("id", 2, "name", "Bob", "email", "bob@example.com", "amount", 200.00),
                Map.of("id", 3, "name", "Charlie", "email", "charlie@example.com", "amount", 0.0),
                Map.of("id", 4, "name", "Diana", "email", "diana@example.com", "amount", 325.75)
        );
        Task task = taskWith(Map.of("transformedRecords", records));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("validCount"));
        assertEquals(1, result.getOutputData().get("invalidCount"));
    }

    @Test
    void handlesNullTransformedRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("transformedRecords", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("validCount"));
        assertEquals(0, result.getOutputData().get("invalidCount"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("validCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
