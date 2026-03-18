package contentenricher.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnrichWorkerTest {

    private final EnrichWorker worker = new EnrichWorker();

    @Test
    void taskDefName() {
        assertEquals("enr_enrich", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void enrichesMergesOriginalAndLookup() {
        Task task = taskWith(Map.of(
                "originalMessage", Map.of("customerId", "C1", "orderId", "O1"),
                "lookupData", Map.of("tier", "gold", "region", "US-WEST")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedMessage");
        assertEquals("C1", enriched.get("customerId"));
        assertEquals("gold", enriched.get("tier"));
        assertEquals("US-WEST", enriched.get("region"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void enrichedContainsIsoTimestamp() {
        Task task = taskWith(Map.of(
                "originalMessage", Map.of("x", 1),
                "lookupData", Map.of("y", 2)));
        TaskResult result = worker.execute(task);

        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedMessage");
        String enrichedAt = (String) enriched.get("enrichedAt");
        assertNotNull(enrichedAt);
        // Real ISO-8601 timestamp from Instant.now()
        assertTrue(enrichedAt.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z"),
                "enrichedAt should be a real ISO-8601 timestamp, got: " + enrichedAt);
    }

    @SuppressWarnings("unchecked")
    @Test
    void lookupDataOverridesOriginalOnConflict() {
        Task task = taskWith(Map.of(
                "originalMessage", Map.of("tier", "bronze"),
                "lookupData", Map.of("tier", "gold")));
        TaskResult result = worker.execute(task);

        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedMessage");
        assertEquals("gold", enriched.get("tier"));
    }

    @Test
    void handlesNullOriginalMessage() {
        Map<String, Object> input = new HashMap<>();
        input.put("originalMessage", null);
        input.put("lookupData", Map.of("tier", "silver"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("enrichedMessage"));
    }

    @Test
    void handlesNullLookupData() {
        Map<String, Object> input = new HashMap<>();
        input.put("originalMessage", Map.of("orderId", "O1"));
        input.put("lookupData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    void preservesAllOriginalFields() {
        Task task = taskWith(Map.of(
                "originalMessage", Map.of("a", 1, "b", 2, "c", 3),
                "lookupData", Map.of("d", 4)));
        TaskResult result = worker.execute(task);

        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedMessage");
        assertEquals(1, enriched.get("a"));
        assertEquals(2, enriched.get("b"));
        assertEquals(3, enriched.get("c"));
        assertEquals(4, enriched.get("d"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
