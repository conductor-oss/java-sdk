package etlbasics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformDataWorkerTest {

    private final TransformDataWorker worker = new TransformDataWorker();

    @Test
    void taskDefName() {
        assertEquals("el_transform_data", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void trimsNames() {
        List<Map<String, Object>> rawRecords = List.of(
                Map.of("id", 1, "name", "  Alice Johnson  ", "email", "alice@example.com", "amount", "100.00")
        );
        Task task = taskWith(Map.of("rawRecords", rawRecords, "rules", Map.of()));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> records =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals("Alice Johnson", records.get(0).get("name"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void lowercasesEmails() {
        List<Map<String, Object>> rawRecords = List.of(
                Map.of("id", 1, "name", "Bob", "email", "BOB@EXAMPLE.COM", "amount", "50.00")
        );
        Task task = taskWith(Map.of("rawRecords", rawRecords, "rules", Map.of()));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> records =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals("bob@example.com", records.get(0).get("email"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void parsesAmountStringsToDoubles() {
        List<Map<String, Object>> rawRecords = List.of(
                Map.of("id", 1, "name", "Test", "email", "test@test.com", "amount", "325.75")
        );
        Task task = taskWith(Map.of("rawRecords", rawRecords, "rules", Map.of()));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> records =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(325.75, (double) records.get(0).get("amount"), 0.001);
    }

    @Test
    void returnsCorrectTransformedCount() {
        List<Map<String, Object>> rawRecords = List.of(
                Map.of("id", 1, "name", "A", "email", "a@b.com", "amount", "10"),
                Map.of("id", 2, "name", "B", "email", "b@b.com", "amount", "20"),
                Map.of("id", 3, "name", "C", "email", "c@b.com", "amount", "30")
        );
        Task task = taskWith(Map.of("rawRecords", rawRecords, "rules", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("transformedCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesInvalidAmountString() {
        List<Map<String, Object>> rawRecords = List.of(
                Map.of("id", 1, "name", "Test", "email", "test@test.com", "amount", "not-a-number")
        );
        Task task = taskWith(Map.of("rawRecords", rawRecords, "rules", Map.of()));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> records =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(0.0, (double) records.get(0).get("amount"), 0.001);
    }

    @Test
    void handlesNullRawRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("rawRecords", null);
        input.put("rules", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("transformedCount"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("transformedCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void preservesRecordId() {
        List<Map<String, Object>> rawRecords = List.of(
                Map.of("id", 42, "name", "Test", "email", "test@test.com", "amount", "10")
        );
        Task task = taskWith(Map.of("rawRecords", rawRecords, "rules", Map.of()));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> records =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(42, records.get(0).get("id"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
