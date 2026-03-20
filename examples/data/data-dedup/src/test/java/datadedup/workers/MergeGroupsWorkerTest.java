package datadedup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MergeGroupsWorkerTest {

    private final MergeGroupsWorker worker = new MergeGroupsWorker();

    @Test
    void taskDefName() {
        assertEquals("dp_merge_groups", worker.getTaskDefName());
    }

    @Test
    void picksFirstRecordFromEachGroup() {
        List<List<Map<String, Object>>> groups = List.of(
                List.of(Map.of("id", 1, "name", "Alice", "dedupKey", "a"),
                        Map.of("id", 3, "name", "Alice S.", "dedupKey", "a")),
                List.of(Map.of("id", 2, "name", "Bob", "dedupKey", "b")));
        Task task = taskWith(Map.of("groups", groups));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> merged =
                (List<Map<String, Object>>) result.getOutputData().get("mergedRecords");
        assertEquals(2, merged.size());
        assertEquals("Alice", merged.get(0).get("name"));
        assertEquals("Bob", merged.get(1).get("name"));
    }

    @Test
    void removesDedupKeyFromOutput() {
        List<List<Map<String, Object>>> groups = List.of(
                List.of(Map.of("id", 1, "name", "Alice", "dedupKey", "key1")));
        Task task = taskWith(Map.of("groups", groups));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> merged =
                (List<Map<String, Object>>) result.getOutputData().get("mergedRecords");
        assertFalse(merged.get(0).containsKey("dedupKey"));
    }

    @Test
    void handlesEmptyGroups() {
        Task task = taskWith(Map.of("groups", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> merged =
                (List<Map<String, Object>>) result.getOutputData().get("mergedRecords");
        assertTrue(merged.isEmpty());
    }

    @Test
    void handlesNullGroups() {
        Map<String, Object> input = new HashMap<>();
        input.put("groups", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> merged =
                (List<Map<String, Object>>) result.getOutputData().get("mergedRecords");
        assertTrue(merged.isEmpty());
    }

    @Test
    void singleGroupSingleRecord() {
        List<List<Map<String, Object>>> groups = List.of(
                List.of(Map.of("id", 1, "name", "Solo", "dedupKey", "x")));
        Task task = taskWith(Map.of("groups", groups));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> merged =
                (List<Map<String, Object>>) result.getOutputData().get("mergedRecords");
        assertEquals(1, merged.size());
        assertEquals("Solo", merged.get(0).get("name"));
    }

    @Test
    void preservesAllFieldsExceptDedupKey() {
        List<List<Map<String, Object>>> groups = List.of(
                List.of(Map.of("id", 1, "name", "Alice", "email", "a@b.com", "dedupKey", "k")));
        Task task = taskWith(Map.of("groups", groups));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> merged =
                (List<Map<String, Object>>) result.getOutputData().get("mergedRecords");
        assertEquals(1, merged.get(0).get("id"));
        assertEquals("Alice", merged.get(0).get("name"));
        assertEquals("a@b.com", merged.get(0).get("email"));
        assertFalse(merged.get(0).containsKey("dedupKey"));
    }

    @Test
    void multipleGroupsMerge() {
        List<List<Map<String, Object>>> groups = List.of(
                List.of(Map.of("id", 1, "dedupKey", "a"), Map.of("id", 2, "dedupKey", "a")),
                List.of(Map.of("id", 3, "dedupKey", "b"), Map.of("id", 4, "dedupKey", "b")),
                List.of(Map.of("id", 5, "dedupKey", "c")));
        Task task = taskWith(Map.of("groups", groups));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> merged =
                (List<Map<String, Object>>) result.getOutputData().get("mergedRecords");
        assertEquals(3, merged.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
