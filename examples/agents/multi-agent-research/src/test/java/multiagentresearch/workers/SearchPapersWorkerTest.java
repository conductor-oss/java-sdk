package multiagentresearch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchPapersWorkerTest {

    private final SearchPapersWorker worker = new SearchPapersWorker();

    @Test
    void taskDefName() {
        assertEquals("ra_search_papers", worker.getTaskDefName());
    }

    @Test
    void returnsTwoFindings() {
        Task task = taskWith(Map.of(
                "queries", List.of("LLM research"),
                "academicDomains", List.of("computer science", "engineering")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) result.getOutputData().get("findings");
        assertNotNull(findings);
        assertEquals(2, findings.size());
    }

    @Test
    void findingsHaveCitations() {
        Task task = taskWith(Map.of(
                "queries", List.of("test"),
                "academicDomains", List.of("cs")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) result.getOutputData().get("findings");
        for (Map<String, Object> finding : findings) {
            assertNotNull(finding.get("citations"));
            assertTrue(((Number) finding.get("citations")).intValue() > 0);
        }
    }

    @Test
    void findingsHaveRequiredFields() {
        Task task = taskWith(Map.of(
                "queries", List.of("test"),
                "academicDomains", List.of("cs")));
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
    void searchEngineIsAcademic() {
        Task task = taskWith(Map.of("queries", List.of("test")));
        TaskResult result = worker.execute(task);

        assertEquals("academic", result.getOutputData().get("searchEngine"));
    }

    @Test
    void totalScannedIs89() {
        Task task = taskWith(Map.of("queries", List.of("test")));
        TaskResult result = worker.execute(task);

        assertEquals(89, result.getOutputData().get("totalScanned"));
    }

    @Test
    void handlesNullQueries() {
        Map<String, Object> input = new HashMap<>();
        input.put("queries", null);
        input.put("academicDomains", null);
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

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
