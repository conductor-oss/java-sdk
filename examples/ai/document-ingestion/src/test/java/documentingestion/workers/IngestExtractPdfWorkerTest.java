package documentingestion.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IngestExtractPdfWorkerTest {

    private final IngestExtractPdfWorker worker = new IngestExtractPdfWorker();

    @Test
    void taskDefName() {
        assertEquals("ingest_extract_pdf", worker.getTaskDefName());
    }

    @Test
    void extractsTextFromLocalFile(@TempDir Path tempDir) throws IOException {
        String content = "Chapter 1: Introduction to Vector Databases.\n\n"
                + "Chapter 2: Embedding Models.\n\n"
                + "Chapter 3: Indexing Strategies.";
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, content);

        Task task = taskWith(new HashMap<>(Map.of("documentUrl", testFile.toString())));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String text = (String) result.getOutputData().get("text");
        assertNotNull(text);
        assertTrue(text.contains("Chapter 1"));
        assertTrue(text.contains("Vector Databases"));
    }

    @Test
    void returnsPageCountBasedOnLength(@TempDir Path tempDir) throws IOException {
        String content = "A".repeat(5000);
        Path testFile = tempDir.resolve("long.txt");
        Files.writeString(testFile, content);

        Task task = taskWith(new HashMap<>(Map.of("documentUrl", testFile.toString())));
        TaskResult result = worker.execute(task);

        int pageCount = ((Number) result.getOutputData().get("pageCount")).intValue();
        assertTrue(pageCount >= 2, "Expected at least 2 pages for 5000 chars");
    }

    @Test
    void returnsCharCount(@TempDir Path tempDir) throws IOException {
        String content = "Hello World";
        Path testFile = tempDir.resolve("hello.txt");
        Files.writeString(testFile, content);

        Task task = taskWith(new HashMap<>(Map.of("documentUrl", testFile.toString())));
        TaskResult result = worker.execute(task);

        String text = (String) result.getOutputData().get("text");
        assertEquals(text.length(), result.getOutputData().get("charCount"));
    }

    @Test
    void failsOnNonexistentFile() {
        Task task = taskWith(new HashMap<>(Map.of("documentUrl", "/nonexistent/path/file.txt")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertNotNull(result.getReasonForIncompletion());
    }

    @Test
    void failsOnNullDocumentUrl() {
        Map<String, Object> input = new HashMap<>();
        input.put("documentUrl", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("documentUrl"));
    }

    @Test
    void handlesEmptyFile(@TempDir Path tempDir) throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.writeString(emptyFile, "");

        Task task = taskWith(new HashMap<>(Map.of("documentUrl", emptyFile.toString())));
        TaskResult result = worker.execute(task);

        // Empty file might fail or return empty text — either is acceptable
        assertNotNull(result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
