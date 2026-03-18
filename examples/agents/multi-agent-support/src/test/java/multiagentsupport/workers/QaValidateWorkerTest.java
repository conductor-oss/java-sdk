package multiagentsupport.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QaValidateWorkerTest {

    private final QaValidateWorker worker = new QaValidateWorker();

    @Test
    void taskDefName() {
        assertEquals("cs_qa_validate", worker.getTaskDefName());
    }

    @Test
    void returnsApproved() {
        Task task = taskWith(Map.of(
                "ticketId", "TKT-1234",
                "category", "bug",
                "customerTier", "premium"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test
    void runsFourChecks() {
        Task task = taskWith(Map.of(
                "ticketId", "TKT-5678",
                "category", "feature",
                "customerTier", "standard"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> checks = (Map<String, Object>) result.getOutputData().get("checks");
        assertNotNull(checks);
        assertEquals(4, checks.size());
        assertEquals(true, checks.get("toneAppropriate"));
        assertEquals(true, checks.get("includesGreeting"));
        assertEquals(true, checks.get("includesNextSteps"));
        assertEquals(true, checks.get("noSensitiveData"));
    }

    @Test
    void returnsFinalResponse() {
        Task task = taskWith(Map.of(
                "ticketId", "TKT-9999",
                "category", "general",
                "customerTier", "enterprise"));
        TaskResult result = worker.execute(task);

        String finalResponse = (String) result.getOutputData().get("finalResponse");
        assertNotNull(finalResponse);
        assertTrue(finalResponse.contains("TKT-9999"));
        assertTrue(finalResponse.contains("general"));
    }

    @Test
    void returnsReviewedAt() {
        Task task = taskWith(Map.of(
                "ticketId", "TKT-0001",
                "category", "bug",
                "customerTier", "standard"));
        TaskResult result = worker.execute(task);

        String reviewedAt = (String) result.getOutputData().get("reviewedAt");
        assertNotNull(reviewedAt);
        assertTrue(reviewedAt.contains("2026"));
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("ticketId", null);
        input.put("category", null);
        input.put("customerTier", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
        assertNotNull(result.getOutputData().get("finalResponse"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("approved"));
        assertNotNull(result.getOutputData().get("checks"));
        assertNotNull(result.getOutputData().get("finalResponse"));
        assertNotNull(result.getOutputData().get("reviewedAt"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("ticketId", "TKT-TEST", "category", "bug"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("approved"));
        assertTrue(result.getOutputData().containsKey("checks"));
        assertTrue(result.getOutputData().containsKey("finalResponse"));
        assertTrue(result.getOutputData().containsKey("reviewedAt"));
    }

    @Test
    void finalResponseIncludesTicketIdAndCategory() {
        Task task = taskWith(Map.of(
                "ticketId", "TKT-ABC",
                "category", "feature",
                "customerTier", "premium"));
        TaskResult result = worker.execute(task);

        String finalResponse = (String) result.getOutputData().get("finalResponse");
        assertTrue(finalResponse.contains("TKT-ABC"));
        assertTrue(finalResponse.contains("feature"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
