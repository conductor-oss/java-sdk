package documentverification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiExtractWorkerTest {

    @Test
    void taskDefName() {
        AiExtractWorker worker = new AiExtractWorker();
        assertEquals("dv_ai_extract", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        AiExtractWorker worker = new AiExtractWorker();
        Task task = taskWith(Map.of("documentId", "DOC-001"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsExtractedMap() {
        AiExtractWorker worker = new AiExtractWorker();
        Task task = taskWith(Map.of("documentId", "DOC-001"));

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("extracted"));
        Object extracted = result.getOutputData().get("extracted");
        assertInstanceOf(Map.class, extracted);
    }

    @SuppressWarnings("unchecked")
    @Test
    void extractedMapContainsAllRequiredFields() {
        AiExtractWorker worker = new AiExtractWorker();
        Task task = taskWith(Map.of("documentId", "DOC-001"));

        TaskResult result = worker.execute(task);

        Map<String, Object> extracted = (Map<String, Object>) result.getOutputData().get("extracted");
        assertEquals("Jane Doe", extracted.get("name"));
        assertEquals("1990-05-15", extracted.get("dateOfBirth"));
        assertEquals("DOC-123456789", extracted.get("documentNumber"));
        assertEquals("2030-05-15", extracted.get("expiryDate"));
        assertEquals("123 Main St, Springfield, IL 62605", extracted.get("address"));
    }

    @Test
    void returnsConfidenceScore() {
        AiExtractWorker worker = new AiExtractWorker();
        Task task = taskWith(Map.of("documentId", "DOC-001"));

        TaskResult result = worker.execute(task);

        assertEquals(0.92, result.getOutputData().get("confidence"));
    }

    @Test
    void worksWithEmptyInput() {
        AiExtractWorker worker = new AiExtractWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("extracted"));
        assertTrue(result.getOutputData().containsKey("confidence"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void extractedMapHasFiveFields() {
        AiExtractWorker worker = new AiExtractWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        Map<String, Object> extracted = (Map<String, Object>) result.getOutputData().get("extracted");
        assertEquals(5, extracted.size());
    }

    @Test
    void isDeterministic() {
        AiExtractWorker worker = new AiExtractWorker();
        Task task1 = taskWith(Map.of("documentId", "DOC-001"));
        Task task2 = taskWith(Map.of("documentId", "DOC-002"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("extracted"), result2.getOutputData().get("extracted"));
        assertEquals(result1.getOutputData().get("confidence"), result2.getOutputData().get("confidence"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
