package datadedup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmitDedupedWorkerTest {

    private final EmitDedupedWorker worker = new EmitDedupedWorker();

    @Test
    void taskDefName() {
        assertEquals("dp_emit_deduped", worker.getTaskDefName());
    }

    @Test
    void emitsDedupedCountAndSummary() {
        List<Map<String, Object>> deduped = List.of(
                Map.of("id", 1, "name", "Alice"),
                Map.of("id", 2, "name", "Bob"),
                Map.of("id", 4, "name", "Charlie"),
                Map.of("id", 6, "name", "Diana"));
        Task task = taskWith(Map.of("deduped", deduped, "originalCount", 6, "dupCount", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4, result.getOutputData().get("dedupedCount"));
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("6"));
        assertTrue(summary.contains("4"));
        assertTrue(summary.contains("2 duplicates removed"));
    }

    @Test
    void summaryFormat() {
        List<Map<String, Object>> deduped = List.of(Map.of("id", 1));
        Task task = taskWith(Map.of("deduped", deduped, "originalCount", 3, "dupCount", 2));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertEquals("Dedup complete: 3 \u2192 1 records (2 duplicates removed)", summary);
    }

    @Test
    void handlesEmptyDeduped() {
        Task task = taskWith(Map.of("deduped", List.of(), "originalCount", 0, "dupCount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("dedupedCount"));
    }

    @Test
    void handlesNullDeduped() {
        Map<String, Object> input = new HashMap<>();
        input.put("deduped", null);
        input.put("originalCount", 5);
        input.put("dupCount", 3);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("dedupedCount"));
    }

    @Test
    void handlesNullCounts() {
        Map<String, Object> input = new HashMap<>();
        input.put("deduped", List.of(Map.of("id", 1)));
        input.put("originalCount", null);
        input.put("dupCount", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("dedupedCount"));
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("0 duplicates removed"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("dedupedCount"));
    }

    @Test
    void zeroDuplicates() {
        List<Map<String, Object>> deduped = List.of(
                Map.of("id", 1), Map.of("id", 2), Map.of("id", 3));
        Task task = taskWith(Map.of("deduped", deduped, "originalCount", 3, "dupCount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("dedupedCount"));
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("0 duplicates removed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
