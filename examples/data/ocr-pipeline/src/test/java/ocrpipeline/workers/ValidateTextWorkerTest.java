package ocrpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateTextWorkerTest {

    private final ValidateTextWorker worker = new ValidateTextWorker();

    @Test
    void taskDefName() {
        assertEquals("oc_validate_text", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void extractsSevenFields() {
        Task task = taskWith(Map.of("rawText", "INVOICE #INV-2024-0892\nTotal: $12,450.00",
                "confidence", 94.7, "documentType", "invoice"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> fields = (Map<String, Object>) result.getOutputData().get("fields");
        assertNotNull(fields);
        assertEquals(7, fields.size());
        assertEquals("INV-2024-0892", fields.get("invoiceNumber"));
        assertEquals("$12,450.00", fields.get("total"));
    }

    @Test
    void returnsValidationScore() {
        Task task = taskWith(Map.of("rawText", "some text", "confidence", 90.0, "documentType", "invoice"));
        TaskResult result = worker.execute(task);

        assertEquals(97.2, result.getOutputData().get("validationScore"));
    }

    @Test
    void trimsCleanText() {
        Task task = taskWith(Map.of("rawText", "  text with spaces  ", "confidence", 90.0, "documentType", "invoice"));
        TaskResult result = worker.execute(task);

        assertEquals("text with spaces", result.getOutputData().get("cleanText"));
    }

    @Test
    void handlesEmptyRawText() {
        Map<String, Object> input = new HashMap<>();
        input.put("rawText", null);
        Task task = taskWith(input);
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
