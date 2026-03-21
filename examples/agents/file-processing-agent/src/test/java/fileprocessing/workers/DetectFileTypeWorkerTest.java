package fileprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectFileTypeWorkerTest {

    private final DetectFileTypeWorker worker = new DetectFileTypeWorker();

    @Test
    void taskDefName() {
        assertEquals("fp_detect_file_type", worker.getTaskDefName());
    }

    @Test
    void detectsPdfFile() {
        Task task = taskWith(Map.of("fileName", "report.pdf", "fileSize", 245760, "mimeType", "application/pdf"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("document", result.getOutputData().get("fileType"));
        assertEquals("pdf_parser", result.getOutputData().get("extractionMethod"));
        assertEquals("text_document", result.getOutputData().get("category"));
        assertEquals("pdf", result.getOutputData().get("extension"));
        assertEquals("application/pdf", result.getOutputData().get("originalMimeType"));
    }

    @Test
    void detectsCsvFile() {
        Task task = taskWith(Map.of("fileName", "data.csv", "mimeType", "text/csv"));
        TaskResult result = worker.execute(task);

        assertEquals("spreadsheet", result.getOutputData().get("fileType"));
        assertEquals("csv_parser", result.getOutputData().get("extractionMethod"));
        assertEquals("structured_data", result.getOutputData().get("category"));
        assertEquals("csv", result.getOutputData().get("extension"));
    }

    @Test
    void detectsPngImage() {
        Task task = taskWith(Map.of("fileName", "photo.png", "mimeType", "image/png"));
        TaskResult result = worker.execute(task);

        assertEquals("image", result.getOutputData().get("fileType"));
        assertEquals("ocr", result.getOutputData().get("extractionMethod"));
        assertEquals("visual", result.getOutputData().get("category"));
    }

    @Test
    void detectsJsonFile() {
        Task task = taskWith(Map.of("fileName", "config.json", "mimeType", "application/json"));
        TaskResult result = worker.execute(task);

        assertEquals("data", result.getOutputData().get("fileType"));
        assertEquals("json_parser", result.getOutputData().get("extractionMethod"));
        assertEquals("structured_data", result.getOutputData().get("category"));
    }

    @Test
    void handlesUnknownExtension() {
        Task task = taskWith(Map.of("fileName", "archive.tar.gz", "mimeType", "application/gzip"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("fileType"));
        assertEquals("raw_text", result.getOutputData().get("extractionMethod"));
        assertEquals("other", result.getOutputData().get("category"));
        assertEquals("gz", result.getOutputData().get("extension"));
    }

    @Test
    void handlesNullFileName() {
        Map<String, Object> input = new HashMap<>();
        input.put("fileName", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("fileType"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("application/octet-stream", result.getOutputData().get("originalMimeType"));
    }

    @Test
    void handlesBlankFileName() {
        Task task = taskWith(Map.of("fileName", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("fileType"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
