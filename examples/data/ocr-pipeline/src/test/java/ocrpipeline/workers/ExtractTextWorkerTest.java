package ocrpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractTextWorkerTest {

    private final ExtractTextWorker worker = new ExtractTextWorker();

    @Test
    void taskDefName() {
        assertEquals("oc_extract_text", worker.getTaskDefName());
    }

    @Test
    void extractsTextWithConfidence() {
        Task task = taskWith(Map.of("processedImage", "preprocessed_data", "language", "en"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("rawText"));
        assertEquals(94.7, result.getOutputData().get("confidence"));
    }

    @Test
    void rawTextContainsInvoiceData() {
        Task task = taskWith(Map.of("processedImage", "preprocessed_data", "language", "en"));
        TaskResult result = worker.execute(task);

        String rawText = (String) result.getOutputData().get("rawText");
        assertTrue(rawText.contains("INV-2024-0892"));
        assertTrue(rawText.contains("Acme Corporation"));
        assertTrue(rawText.contains("$12,450.00"));
    }

    @Test
    void characterCountMatchesTextLength() {
        Task task = taskWith(Map.of("processedImage", "preprocessed_data", "language", "en"));
        TaskResult result = worker.execute(task);

        String rawText = (String) result.getOutputData().get("rawText");
        assertEquals(rawText.length(), result.getOutputData().get("characterCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
