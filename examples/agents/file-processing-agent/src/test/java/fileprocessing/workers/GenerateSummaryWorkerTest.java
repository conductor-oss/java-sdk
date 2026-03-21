package fileprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateSummaryWorkerTest {

    private final GenerateSummaryWorker worker = new GenerateSummaryWorker();

    @Test
    void taskDefName() {
        assertEquals("fp_generate_summary", worker.getTaskDefName());
    }

    @Test
    void generatesSummaryWithAnalysis() {
        Task task = taskWith(Map.of(
                "fileName", "acme_q4_2025_financial_report.pdf",
                "fileType", "document",
                "analysis", Map.of(
                        "documentType", "financial_report",
                        "company", "Acme Corporation",
                        "timePeriod", "Q4 2025",
                        "sentiment", "positive"),
                "keyFindings", List.of(
                        "Record revenue of $2.4B",
                        "Enterprise segment at 67%")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertTrue(summary.contains("acme_q4_2025_financial_report.pdf"));
        assertTrue(summary.contains("financial_report"));
        assertTrue(summary.contains("Acme Corporation"));
        assertTrue(summary.contains("Q4 2025"));
        assertTrue(summary.contains("positive"));
    }

    @Test
    void returnsConfidenceValue() {
        Task task = taskWith(Map.of(
                "fileName", "report.pdf",
                "fileType", "document",
                "analysis", Map.of("documentType", "report"),
                "keyFindings", List.of("Finding 1")));
        TaskResult result = worker.execute(task);

        assertEquals(0.93, result.getOutputData().get("confidence"));
    }

    @Test
    void returnsSummaryWordCount() {
        Task task = taskWith(Map.of(
                "fileName", "report.pdf",
                "fileType", "document",
                "analysis", Map.of("documentType", "report"),
                "keyFindings", List.of("Finding 1")));
        TaskResult result = worker.execute(task);

        int wordCount = (int) result.getOutputData().get("summaryWordCount");
        assertTrue(wordCount > 0);
    }

    @Test
    void returnsDocumentType() {
        Task task = taskWith(Map.of(
                "fileName", "report.pdf",
                "fileType", "document",
                "analysis", Map.of("documentType", "financial_report"),
                "keyFindings", List.of("Finding 1")));
        TaskResult result = worker.execute(task);

        assertEquals("financial_report", result.getOutputData().get("documentType"));
    }

    @Test
    void handlesNullAnalysis() {
        Map<String, Object> input = new HashMap<>();
        input.put("fileName", "report.pdf");
        input.put("analysis", null);
        input.put("keyFindings", List.of("Finding 1"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertTrue(summary.contains("unknown"));
    }

    @Test
    void handlesMissingFileName() {
        Task task = taskWith(Map.of(
                "analysis", Map.of("documentType", "report"),
                "keyFindings", List.of("Finding 1")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("unknown_file"));
    }

    @Test
    void handlesNullKeyFindings() {
        Map<String, Object> input = new HashMap<>();
        input.put("fileName", "report.pdf");
        input.put("analysis", Map.of("documentType", "report"));
        input.put("keyFindings", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("summary"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("summary"));
        assertEquals(0.93, result.getOutputData().get("confidence"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
