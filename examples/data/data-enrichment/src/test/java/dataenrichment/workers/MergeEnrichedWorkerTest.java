package dataenrichment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MergeEnrichedWorkerTest {

    private final MergeEnrichedWorker worker = new MergeEnrichedWorker();

    @Test
    void taskDefName() {
        assertEquals("dr_merge_enriched", worker.getTaskDefName());
    }

    @Test
    void producesEnrichedCount() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "geo", Map.of("city", "SF"), "company", Map.of("name", "Acme"), "credit", Map.of("score", 750)),
                Map.of("id", 2, "name", "Bob", "geo", Map.of("city", "NY"), "company", Map.of("name", "Globex"), "credit", Map.of("score", 700)),
                Map.of("id", 3, "name", "Charlie", "geo", Map.of("city", "Chicago"), "company", Map.of("name", "Initech"), "credit", Map.of("score", 650)));
        Task task = taskWith(Map.of("records", records, "originalCount", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("enrichedCount"));
    }

    @Test
    void countsActualEnrichmentFieldsAdded() {
        // Records with geo, company, and credit enrichment
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "geo", Map.of("city", "SF"), "company", Map.of("name", "Acme"), "credit", Map.of("score", 750)));
        Task task = taskWith(Map.of("records", records, "originalCount", 1));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("fieldsAdded")); // geo, company, credit
    }

    @Test
    void countsOnlyPresentEnrichmentFields() {
        // Records with only geo enrichment, no company or credit
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "geo", Map.of("city", "SF")));
        Task task = taskWith(Map.of("records", records, "originalCount", 1));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("fieldsAdded")); // only geo
    }

    @Test
    void summaryContainsCountAndSources() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "geo", Map.of("city", "SF"), "company", Map.of("name", "Acme"), "credit", Map.of("score", 750)),
                Map.of("id", 2, "name", "Bob", "geo", Map.of("city", "NY"), "company", Map.of("name", "Globex"), "credit", Map.of("score", 700)));
        Task task = taskWith(Map.of("records", records, "originalCount", 2));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertTrue(summary.contains("2/2"));
        assertTrue(summary.contains("3 data sources"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void outputContainsRecords() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice"));
        Task task = taskWith(Map.of("records", records, "originalCount", 1));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> outputRecords =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertNotNull(outputRecords);
        assertEquals(1, outputRecords.size());
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of(), "originalCount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("enrichedCount"));
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("0/0"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("originalCount", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("enrichedCount"));
    }

    @Test
    void handlesMissingOriginalCount() {
        List<Map<String, Object>> records = List.of(Map.of("id", 1, "name", "Alice"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("enrichedCount"));
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("1/0"));
    }

    @Test
    void summaryFormatIsCorrect() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "geo", Map.of(), "company", Map.of(), "credit", Map.of()),
                Map.of("id", 2, "geo", Map.of(), "company", Map.of(), "credit", Map.of()),
                Map.of("id", 3, "geo", Map.of(), "company", Map.of(), "credit", Map.of()));
        Task task = taskWith(Map.of("records", records, "originalCount", 3));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertEquals("Enriched 3/3 records with 3 data sources", summary);
    }

    @Test
    void countsDnsFieldIfPresent() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "dns", Map.of("hostname", "localhost", "ip", "127.0.0.1")));
        Task task = taskWith(Map.of("records", records, "originalCount", 1));
        TaskResult result = worker.execute(task);

        // dns is a valid enrichment field
        int fieldsAdded = ((Number) result.getOutputData().get("fieldsAdded")).intValue();
        assertEquals(1, fieldsAdded);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
