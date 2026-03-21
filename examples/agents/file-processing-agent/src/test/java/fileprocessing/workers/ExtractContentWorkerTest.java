package fileprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractContentWorkerTest {

    private final ExtractContentWorker worker = new ExtractContentWorker();

    @Test
    void taskDefName() {
        assertEquals("fp_extract_content", worker.getTaskDefName());
    }

    @Test
    void returnsContentWithTitle() {
        Task task = taskWith(Map.of("fileName", "report.pdf", "fileType", "document", "extractionMethod", "pdf_parser"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) result.getOutputData().get("content");
        assertNotNull(content);
        assertEquals("Q4 2025 Financial Report — Acme Corporation", content.get("title"));
        assertEquals(24, content.get("pages"));
    }

    @Test
    void returnsContentWithFourSections() {
        Task task = taskWith(Map.of("fileName", "report.pdf", "fileType", "document", "extractionMethod", "pdf_parser"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) result.getOutputData().get("content");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> sections = (List<Map<String, String>>) content.get("sections");
        assertNotNull(sections);
        assertEquals(4, sections.size());
        assertEquals("Executive Summary", sections.get(0).get("heading"));
    }

    @Test
    void returnsMetadata() {
        Task task = taskWith(Map.of("fileName", "report.pdf", "fileType", "document", "extractionMethod", "pdf_parser"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.getOutputData().get("metadata");
        assertNotNull(metadata);
        assertEquals(156, metadata.get("wordCount"));
        assertEquals(24, metadata.get("pageCount"));
        assertEquals(4, metadata.get("sectionCount"));
        assertEquals(true, metadata.get("hasImages"));
        assertEquals(true, metadata.get("hasTables"));
        assertEquals("en", metadata.get("language"));
    }

    @Test
    void handlesMissingFileName() {
        Task task = taskWith(Map.of("fileType", "document"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("content"));
        assertNotNull(result.getOutputData().get("metadata"));
    }

    @Test
    void handlesNullFileName() {
        Map<String, Object> input = new HashMap<>();
        input.put("fileName", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesBlankFileName() {
        Task task = taskWith(Map.of("fileName", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("content"));
    }

    @Test
    void handlesMissingExtractionMethod() {
        Task task = taskWith(Map.of("fileName", "report.pdf"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("content"));
        assertNotNull(result.getOutputData().get("metadata"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
