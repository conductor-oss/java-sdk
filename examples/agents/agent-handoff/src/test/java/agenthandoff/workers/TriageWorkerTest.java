package agenthandoff.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TriageWorkerTest {

    private final TriageWorker worker = new TriageWorker();

    @Test
    void taskDefName() {
        assertEquals("ah_triage", worker.getTaskDefName());
    }

    // --- Technical classification ---

    @Test
    void classifiesTechnicalByApiKeyword() {
        Task task = taskWith(Map.of("customerId", "CUST-001", "message", "My api calls are failing"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("technical", result.getOutputData().get("category"));
        assertEquals("normal", result.getOutputData().get("urgency"));
        assertNotNull(result.getOutputData().get("notes"));
    }

    @Test
    void classifiesTechnicalByErrorKeyword() {
        Task task = taskWith(Map.of("customerId", "CUST-002", "message", "I keep getting error messages"));
        TaskResult result = worker.execute(task);

        assertEquals("technical", result.getOutputData().get("category"));
    }

    @Test
    void classifiesTechnicalByTimeoutKeyword() {
        Task task = taskWith(Map.of("customerId", "CUST-003", "message", "Requests timeout after 30 seconds"));
        TaskResult result = worker.execute(task);

        assertEquals("technical", result.getOutputData().get("category"));
    }

    @Test
    void caseInsensitiveClassification() {
        Task task = taskWith(Map.of("customerId", "CUST-011", "message", "My API is broken"));
        TaskResult result = worker.execute(task);

        assertEquals("technical", result.getOutputData().get("category"));
    }

    // --- Billing classification ---

    @Test
    void classifiesBillingByBillKeyword() {
        Task task = taskWith(Map.of("customerId", "CUST-004", "message", "I have a question about my bill"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("billing", result.getOutputData().get("category"));
    }

    @Test
    void classifiesBillingByChargeKeyword() {
        Task task = taskWith(Map.of("customerId", "CUST-005", "message", "There is an unexpected charge on my account"));
        TaskResult result = worker.execute(task);

        assertEquals("billing", result.getOutputData().get("category"));
    }

    @Test
    void classifiesBillingByInvoiceKeyword() {
        Task task = taskWith(Map.of("customerId", "CUST-006", "message", "Please send me an invoice"));
        TaskResult result = worker.execute(task);

        assertEquals("billing", result.getOutputData().get("category"));
    }

    // --- General classification ---

    @Test
    void classifiesGeneralForUnknownMessage() {
        Task task = taskWith(Map.of("customerId", "CUST-007", "message", "I would like to know about your plans"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("general", result.getOutputData().get("category"));
    }

    // --- Confidence scores ---

    @Test
    void confidenceIsBaselineForGeneral() {
        Task task = taskWith(Map.of("customerId", "CUST-020", "message", "Hello"));
        TaskResult result = worker.execute(task);

        assertEquals(0.6, result.getOutputData().get("confidence"));
    }

    @Test
    void confidenceIncreasesWithMoreKeywords() {
        // Single keyword: 0.75
        Task single = taskWith(Map.of("customerId", "CUST-021", "message", "There is an error"));
        TaskResult singleResult = worker.execute(single);
        assertEquals(0.75, singleResult.getOutputData().get("confidence"));

        // Two keywords: 0.80
        Task two = taskWith(Map.of("customerId", "CUST-022", "message", "API error happening"));
        TaskResult twoResult = worker.execute(two);
        assertEquals(0.80, twoResult.getOutputData().get("confidence"));

        // Three keywords: 0.85
        Task three = taskWith(Map.of("customerId", "CUST-023", "message", "API error timeout"));
        TaskResult threeResult = worker.execute(three);
        assertEquals(0.85, threeResult.getOutputData().get("confidence"));
    }

    @Test
    void confidenceCapsAtNinetyFive() {
        // Many keywords should cap at 0.95
        Task task = taskWith(Map.of("customerId", "CUST-024",
                "message", "api error timeout crash bug 500 latency outage"));
        TaskResult result = worker.execute(task);

        assertEquals(0.95, result.getOutputData().get("confidence"));
    }

    // --- Urgency ---

    @Test
    void urgencyNormalByDefault() {
        Task task = taskWith(Map.of("customerId", "CUST-030", "message", "My bill seems wrong"));
        TaskResult result = worker.execute(task);

        assertEquals("normal", result.getOutputData().get("urgency"));
    }

    @Test
    void urgencyHighWithThreeOrMoreKeywords() {
        Task task = taskWith(Map.of("customerId", "CUST-031",
                "message", "API error and timeout happening"));
        TaskResult result = worker.execute(task);

        assertEquals("high", result.getOutputData().get("urgency"));
    }

    @Test
    void urgencyHighWithUrgentKeyword() {
        Task task = taskWith(Map.of("customerId", "CUST-032",
                "message", "This is urgent, my account is broken"));
        TaskResult result = worker.execute(task);

        assertEquals("high", result.getOutputData().get("urgency"));
    }

    // --- Matched keywords ---

    @Test
    void returnsMatchedKeywords() {
        Task task = taskWith(Map.of("customerId", "CUST-040",
                "message", "API timeout happening"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> matched = (List<String>) result.getOutputData().get("matchedKeywords");
        assertNotNull(matched);
        assertTrue(matched.contains("api"));
        assertTrue(matched.contains("timeout"));
    }

    @Test
    void generalReturnsEmptyMatchedKeywords() {
        Task task = taskWith(Map.of("customerId", "CUST-041", "message", "Hello there"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> matched = (List<String>) result.getOutputData().get("matchedKeywords");
        assertNotNull(matched);
        assertTrue(matched.isEmpty());
    }

    // --- Output shape ---

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("customerId", "CUST-008", "message", "Hello"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("category"));
        assertTrue(result.getOutputData().containsKey("urgency"));
        assertTrue(result.getOutputData().containsKey("notes"));
        assertTrue(result.getOutputData().containsKey("confidence"));
        assertTrue(result.getOutputData().containsKey("matchedKeywords"));
        assertEquals(5, result.getOutputData().size());
    }

    // --- Null / blank / missing inputs ---

    @Test
    void handlesNullCustomerId() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", null);
        input.put("message", "Some message");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullMessage() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", "CUST-009");
        input.put("message", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("general", result.getOutputData().get("category"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("general", result.getOutputData().get("category"));
    }

    @Test
    void handlesBlankMessage() {
        Task task = taskWith(Map.of("customerId", "CUST-010", "message", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("general", result.getOutputData().get("category"));
    }

    // --- Determinism ---

    @Test
    void sameInputProducesSameOutput() {
        Task task1 = taskWith(Map.of("customerId", "CUST-050", "message", "API timeout error"));
        Task task2 = taskWith(Map.of("customerId", "CUST-050", "message", "API timeout error"));

        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("category"), r2.getOutputData().get("category"));
        assertEquals(r1.getOutputData().get("confidence"), r2.getOutputData().get("confidence"));
        assertEquals(r1.getOutputData().get("urgency"), r2.getOutputData().get("urgency"));
        assertEquals(r1.getOutputData().get("matchedKeywords"), r2.getOutputData().get("matchedKeywords"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
