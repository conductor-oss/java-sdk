package datadedup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FindDuplicatesWorkerTest {

    private final FindDuplicatesWorker worker = new FindDuplicatesWorker();

    @Test
    void taskDefName() {
        assertEquals("dp_find_duplicates", worker.getTaskDefName());
    }

    @Test
    void findsDuplicateGroups() {
        List<Map<String, Object>> keyedRecords = List.of(
                Map.of("id", 1, "dedupKey", "alice@example.com"),
                Map.of("id", 2, "dedupKey", "bob@example.com"),
                Map.of("id", 3, "dedupKey", "alice@example.com"));
        Task task = taskWith(Map.of("keyedRecords", keyedRecords));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("groupCount"));
        assertEquals(1, result.getOutputData().get("duplicateCount"));
    }

    @Test
    void returnsAllGroups() {
        List<Map<String, Object>> keyedRecords = List.of(
                Map.of("id", 1, "dedupKey", "a"),
                Map.of("id", 2, "dedupKey", "b"),
                Map.of("id", 3, "dedupKey", "a"));
        Task task = taskWith(Map.of("keyedRecords", keyedRecords));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<List<Map<String, Object>>> groups =
                (List<List<Map<String, Object>>>) result.getOutputData().get("groups");
        assertEquals(2, groups.size());
    }

    @Test
    void noDuplicates() {
        List<Map<String, Object>> keyedRecords = List.of(
                Map.of("id", 1, "dedupKey", "a"),
                Map.of("id", 2, "dedupKey", "b"),
                Map.of("id", 3, "dedupKey", "c"));
        Task task = taskWith(Map.of("keyedRecords", keyedRecords));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("groupCount"));
        assertEquals(0, result.getOutputData().get("duplicateCount"));
    }

    @Test
    void allDuplicates() {
        List<Map<String, Object>> keyedRecords = List.of(
                Map.of("id", 1, "dedupKey", "same"),
                Map.of("id", 2, "dedupKey", "same"),
                Map.of("id", 3, "dedupKey", "same"));
        Task task = taskWith(Map.of("keyedRecords", keyedRecords));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("groupCount"));
        assertEquals(2, result.getOutputData().get("duplicateCount"));
        @SuppressWarnings("unchecked")
        List<List<Map<String, Object>>> groups =
                (List<List<Map<String, Object>>>) result.getOutputData().get("groups");
        assertEquals(1, groups.size());
        assertEquals(3, groups.get(0).size());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of("keyedRecords", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("groupCount"));
        assertEquals(0, result.getOutputData().get("duplicateCount"));
    }

    @Test
    void handlesNullInput() {
        Map<String, Object> input = new HashMap<>();
        input.put("keyedRecords", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("groupCount"));
        assertEquals(0, result.getOutputData().get("duplicateCount"));
    }

    @Test
    void multipleDuplicateGroups() {
        List<Map<String, Object>> keyedRecords = List.of(
                Map.of("id", 1, "dedupKey", "a"),
                Map.of("id", 2, "dedupKey", "b"),
                Map.of("id", 3, "dedupKey", "a"),
                Map.of("id", 4, "dedupKey", "b"),
                Map.of("id", 5, "dedupKey", "c"));
        Task task = taskWith(Map.of("keyedRecords", keyedRecords));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("groupCount"));
        assertEquals(2, result.getOutputData().get("duplicateCount"));
        @SuppressWarnings("unchecked")
        List<List<Map<String, Object>>> groups =
                (List<List<Map<String, Object>>>) result.getOutputData().get("groups");
        assertEquals(3, groups.size());
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("groupCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
