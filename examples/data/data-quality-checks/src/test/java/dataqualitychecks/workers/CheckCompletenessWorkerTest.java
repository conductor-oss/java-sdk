package dataqualitychecks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckCompletenessWorkerTest {

    private final CheckCompletenessWorker worker = new CheckCompletenessWorker();

    @Test
    void taskDefName() {
        assertEquals("qc_check_completeness", worker.getTaskDefName());
    }

    @Test
    void allFieldsFilled() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@test.com", "status", "active"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1.0, result.getOutputData().get("score"));
        assertEquals(4, result.getOutputData().get("filledFields"));
        assertEquals(4, result.getOutputData().get("totalFields"));
    }

    @Test
    void missingFieldsReduceScore() {
        Map<String, Object> record = new HashMap<>();
        record.put("id", 1);
        record.put("name", "Alice");
        record.put("email", null);
        record.put("status", null);
        List<Map<String, Object>> records = List.of(record);
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(0.5, result.getOutputData().get("score"));
        assertEquals(2, result.getOutputData().get("filledFields"));
    }

    @Test
    void emptyStringCountsAsNotFilled() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "", "email", "a@b.com", "status", "active"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("filledFields"));
    }

    @Test
    void handlesEmptyRecordList() {
        Task task = taskWith(Map.of("records", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, result.getOutputData().get("score"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(0.0, result.getOutputData().get("score"));
        assertEquals(0, result.getOutputData().get("totalFields"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void reportsMissingFieldDetails() {
        Map<String, Object> record = new HashMap<>();
        record.put("id", 1);
        record.put("name", "Alice");
        record.put("email", null);
        record.put("status", null);
        List<Map<String, Object>> records = List.of(record);
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> missingDetails =
                (List<Map<String, Object>>) result.getOutputData().get("missingDetails");
        assertNotNull(missingDetails);
        assertEquals(1, missingDetails.size());
        List<String> missing = (List<String>) missingDetails.get(0).get("missingFields");
        assertTrue(missing.contains("email"));
        assertTrue(missing.contains("status"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void noMissingDetailsWhenAllComplete() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@test.com", "status", "active"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> missingDetails =
                (List<Map<String, Object>>) result.getOutputData().get("missingDetails");
        assertTrue(missingDetails.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
