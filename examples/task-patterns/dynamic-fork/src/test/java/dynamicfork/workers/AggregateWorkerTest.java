package dynamicfork.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateWorkerTest {

    private final AggregateWorker worker = new AggregateWorker();

    @Test
    void taskDefName() {
        assertEquals("df_aggregate", worker.getTaskDefName());
    }

    @Test
    void aggregatesMultipleResults() {
        Map<String, Object> joinOutput = new HashMap<>();
        joinOutput.put("fetch_0_ref", Map.of(
                "url", "https://a.com", "status", 200, "size", 1500, "loadTime", 100));
        joinOutput.put("fetch_1_ref", Map.of(
                "url", "https://b.com", "status", 200, "size", 2500, "loadTime", 150));
        joinOutput.put("fetch_2_ref", Map.of(
                "url", "https://c.com", "status", 200, "size", 3000, "loadTime", 200));

        Task task = taskWith(Map.of("joinOutput", joinOutput));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("totalProcessed"));
        assertEquals(7000, result.getOutputData().get("totalSize"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(3, results.size());

        // Results are sorted by key, so fetch_0_ref first
        assertEquals("https://a.com", results.get(0).get("url"));
        assertEquals(200, results.get(0).get("status"));
        assertEquals(1500, results.get(0).get("size"));

        assertEquals("https://b.com", results.get(1).get("url"));
        assertEquals("https://c.com", results.get(2).get("url"));
    }

    @Test
    void aggregatesSingleResult() {
        Map<String, Object> joinOutput = new HashMap<>();
        joinOutput.put("fetch_0_ref", Map.of(
                "url", "https://only.com", "status", 200, "size", 4096, "loadTime", 50));

        Task task = taskWith(Map.of("joinOutput", joinOutput));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("totalProcessed"));
        assertEquals(4096, result.getOutputData().get("totalSize"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(1, results.size());
        assertEquals("https://only.com", results.get(0).get("url"));
    }

    @Test
    void handlesEmptyJoinOutput() {
        Task task = taskWith(Map.of("joinOutput", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalProcessed"));
        assertEquals(0, result.getOutputData().get("totalSize"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void handlesNullJoinOutput() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalProcessed"));
        assertEquals(0, result.getOutputData().get("totalSize"));
    }

    @Test
    void ignoresNonFetchEntries() {
        Map<String, Object> joinOutput = new HashMap<>();
        joinOutput.put("fetch_0_ref", Map.of(
                "url", "https://a.com", "status", 200, "size", 1000, "loadTime", 50));
        joinOutput.put("some_other_task_ref", Map.of(
                "url", "https://other.com", "status", 200, "size", 9999, "loadTime", 999));

        Task task = taskWith(Map.of("joinOutput", joinOutput));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("totalProcessed"));
        assertEquals(1000, result.getOutputData().get("totalSize"));
    }

    @Test
    void resultsAreOrderedByIndex() {
        Map<String, Object> joinOutput = new HashMap<>();
        // Add in reverse order to verify sorting
        joinOutput.put("fetch_2_ref", Map.of(
                "url", "https://c.com", "status", 200, "size", 3000, "loadTime", 200));
        joinOutput.put("fetch_0_ref", Map.of(
                "url", "https://a.com", "status", 200, "size", 1000, "loadTime", 100));
        joinOutput.put("fetch_1_ref", Map.of(
                "url", "https://b.com", "status", 200, "size", 2000, "loadTime", 150));

        Task task = taskWith(Map.of("joinOutput", joinOutput));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals("https://a.com", results.get(0).get("url"));
        assertEquals("https://b.com", results.get(1).get("url"));
        assertEquals("https://c.com", results.get(2).get("url"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
