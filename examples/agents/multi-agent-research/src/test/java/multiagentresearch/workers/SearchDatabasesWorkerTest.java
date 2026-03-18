package multiagentresearch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchDatabasesWorkerTest {

    private final SearchDatabasesWorker worker = new SearchDatabasesWorker();

    @Test
    void taskDefName() {
        assertEquals("ra_search_databases", worker.getTaskDefName());
    }

    @Test
    void returnsThreeFindings() {
        Task task = taskWith(Map.of(
                "queries", List.of("LLM adoption"),
                "databases", List.of("internal-reports", "market-data", "patents")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) result.getOutputData().get("findings");
        assertNotNull(findings);
        assertEquals(3, findings.size());
    }

    @Test
    void findingsHaveRequiredFields() {
        Task task = taskWith(Map.of(
                "queries", List.of("test"),
                "databases", List.of("db1")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) result.getOutputData().get("findings");
        for (Map<String, Object> finding : findings) {
            assertNotNull(finding.get("source"));
            assertNotNull(finding.get("title"));
            assertNotNull(finding.get("year"));
            assertNotNull(finding.get("keyPoint"));
            assertNotNull(finding.get("credibility"));
            assertTrue(((Number) finding.get("credibility")).doubleValue() > 0.0);
            assertTrue(((Number) finding.get("credibility")).doubleValue() <= 1.0);
        }
    }

    @Test
    void searchEngineIsInternal() {
        Task task = taskWith(Map.of("queries", List.of("test")));
        TaskResult result = worker.execute(task);

        assertEquals("internal", result.getOutputData().get("searchEngine"));
    }

    @Test
    void totalScannedIs234() {
        Task task = taskWith(Map.of("queries", List.of("test")));
        TaskResult result = worker.execute(task);

        assertEquals(234, result.getOutputData().get("totalScanned"));
    }

    @Test
    void handlesNullQueries() {
        Map<String, Object> input = new HashMap<>();
        input.put("queries", null);
        input.put("databases", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("findings"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("findings"));
    }

    @Test
    void findingSourcesAreDistinct() {
        Task task = taskWith(Map.of("queries", List.of("test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) result.getOutputData().get("findings");
        long distinctSources = findings.stream()
                .map(f -> f.get("source"))
                .distinct()
                .count();
        assertEquals(findings.size(), distinctSources, "Each finding should have a distinct source");
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
