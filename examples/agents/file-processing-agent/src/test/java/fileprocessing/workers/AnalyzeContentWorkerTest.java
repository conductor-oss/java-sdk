package fileprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeContentWorkerTest {

    private final AnalyzeContentWorker worker = new AnalyzeContentWorker();

    @Test
    void taskDefName() {
        assertEquals("fp_analyze_content", worker.getTaskDefName());
    }

    @Test
    void returnsAnalysisWithDocumentType() {
        Task task = taskWith(Map.of(
                "content", Map.of("title", "Q4 2025 Financial Report"),
                "metadata", Map.of("wordCount", 156),
                "fileType", "document"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) result.getOutputData().get("analysis");
        assertNotNull(analysis);
        assertEquals("financial_report", analysis.get("documentType"));
        assertEquals("Q4 2025", analysis.get("timePeriod"));
        assertEquals("Acme Corporation", analysis.get("company"));
        assertEquals("positive", analysis.get("sentiment"));
        assertEquals(0.82, analysis.get("sentimentScore"));
    }

    @Test
    void returnsTopics() {
        Task task = taskWith(Map.of(
                "content", Map.of("title", "Report"),
                "fileType", "document"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) result.getOutputData().get("analysis");
        @SuppressWarnings("unchecked")
        List<String> topics = (List<String>) analysis.get("topics");
        assertNotNull(topics);
        assertEquals(5, topics.size());
        assertTrue(topics.contains("revenue"));
        assertTrue(topics.contains("AI"));
    }

    @Test
    void returnsFiveKeyFindings() {
        Task task = taskWith(Map.of(
                "content", Map.of("title", "Report"),
                "fileType", "document"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> keyFindings = (List<String>) result.getOutputData().get("keyFindings");
        assertNotNull(keyFindings);
        assertEquals(5, keyFindings.size());
        assertTrue(keyFindings.get(0).contains("$2.4B"));
    }

    @Test
    void returnsNamedEntities() {
        Task task = taskWith(Map.of(
                "content", Map.of("title", "Report"),
                "fileType", "document"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) result.getOutputData().get("analysis");
        @SuppressWarnings("unchecked")
        Map<String, List<String>> entities = (Map<String, List<String>>) analysis.get("namedEntities");
        assertNotNull(entities);
        assertTrue(entities.get("organizations").contains("Acme Corporation"));
        assertTrue(entities.get("monetaryValues").contains("$2.4B"));
        assertTrue(entities.get("dates").contains("Q4 2025"));
    }

    @Test
    void handlesNullFileType() {
        Map<String, Object> input = new HashMap<>();
        input.put("content", Map.of("title", "Report"));
        input.put("fileType", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingContent() {
        Task task = taskWith(Map.of("fileType", "document"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("analysis"));
        assertNotNull(result.getOutputData().get("keyFindings"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesBlankFileType() {
        Task task = taskWith(Map.of("fileType", "  ", "content", Map.of("title", "Test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
