package terminatetask.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateWorkerTest {

    private final ValidateWorker worker = new ValidateWorker();

    @Test
    void taskDefName() {
        assertEquals("term_validate", worker.getTaskDefName());
    }

    @Test
    void validOrderPassesAllChecks() {
        Task task = taskWith(Map.of("orderId", "ORD-001", "amount", 500, "currency", "USD"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
        assertNull(result.getOutputData().get("reason"));
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertTrue(errors.isEmpty());
    }

    @Test
    void validWithEurCurrency() {
        Task task = taskWith(Map.of("orderId", "ORD-010", "amount", 9999, "currency", "EUR"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test
    void validWithGbpCurrency() {
        Task task = taskWith(Map.of("orderId", "ORD-011", "amount", 1, "currency", "GBP"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test
    void validAtExactLimit() {
        Task task = taskWith(Map.of("orderId", "ORD-012", "amount", 10000, "currency", "USD"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test
    void rejectsAmountExceedingLimit() {
        Task task = taskWith(Map.of("orderId", "ORD-002", "amount", 50000, "currency", "USD"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("valid"));
        String reason = (String) result.getOutputData().get("reason");
        assertNotNull(reason);
        assertTrue(reason.contains("Amount exceeds limit"));
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertEquals(1, errors.size());
    }

    @Test
    void rejectsUnsupportedCurrency() {
        Task task = taskWith(Map.of("orderId", "ORD-003", "amount", 100, "currency", "BTC"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
        String reason = (String) result.getOutputData().get("reason");
        assertTrue(reason.contains("Unsupported currency: BTC"));
    }

    @Test
    void rejectsNegativeAmount() {
        Task task = taskWith(Map.of("orderId", "ORD-004", "amount", -10, "currency", "USD"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
        String reason = (String) result.getOutputData().get("reason");
        assertTrue(reason.contains("Amount must be positive"));
    }

    @Test
    void rejectsZeroAmount() {
        Task task = taskWith(Map.of("orderId", "ORD-005", "amount", 0, "currency", "USD"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
        assertTrue(((String) result.getOutputData().get("reason")).contains("Amount must be positive"));
    }

    @Test
    void multipleErrorsCombined() {
        Task task = taskWith(Map.of("orderId", "ORD-006", "amount", 50000, "currency", "BTC"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
        String reason = (String) result.getOutputData().get("reason");
        assertTrue(reason.contains("Amount exceeds limit"));
        assertTrue(reason.contains("Unsupported currency: BTC"));
        assertTrue(reason.contains("; "), "Multiple errors should be joined with semicolons");

        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        assertEquals(2, errors.size());
    }

    @Test
    void allThreeErrorsWhenEverythingWrong() {
        Task task = taskWith(Map.of("orderId", "ORD-007", "amount", -5, "currency", "XYZ"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.getOutputData().get("errors");
        // Negative amount triggers both "must be positive" — amount <= 0 is not > 10000 so only 2 errors
        assertEquals(2, errors.size());
    }

    @Test
    void nullCurrencyIsRejected() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "ORD-008");
        input.put("amount", 100);
        input.put("currency", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
        String reason = (String) result.getOutputData().get("reason");
        assertTrue(reason.contains("Unsupported currency"));
    }

    @Test
    void statusIsAlwaysCompleted() {
        // Even for invalid input, the task itself completes — the SWITCH/TERMINATE
        // in the workflow handles the early exit logic.
        Task task = taskWith(Map.of("orderId", "ORD-009", "amount", -999, "currency", "FAKE"));
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
