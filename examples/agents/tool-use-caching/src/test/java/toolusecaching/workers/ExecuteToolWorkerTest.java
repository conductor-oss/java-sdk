package toolusecaching.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteToolWorkerTest {

    private final ExecuteToolWorker worker = new ExecuteToolWorker();

    @Test
    void taskDefName() {
        assertEquals("uc_execute_tool", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void executesCurrencyConversion() {
        Task task = taskWith(Map.of(
                "toolName", "currency_exchange",
                "toolArgs", Map.of("from", "USD", "to", "EUR", "amount", 1000)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> convResult = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(convResult);
        assertEquals("USD", convResult.get("baseCurrency"));
        assertEquals("EUR", convResult.get("targetCurrency"));
        assertEquals(0.92, convResult.get("rate"));
        assertEquals(1000.0, convResult.get("amount"));
        assertEquals(920.0, convResult.get("convertedAmount"));
        assertEquals("exchange_rates_api", convResult.get("provider"));
        assertEquals("2026-03-08T10:00:00Z", convResult.get("timestamp"));
    }

    @Test
    void returnsExecutionTimeMs() {
        Task task = taskWith(Map.of(
                "toolName", "currency_exchange",
                "toolArgs", Map.of("from", "USD", "to", "EUR", "amount", 500)));
        TaskResult result = worker.execute(task);

        assertEquals(312, result.getOutputData().get("executionTimeMs"));
    }

    @Test
    void returnsApiCallMadeTrue() {
        Task task = taskWith(Map.of(
                "toolName", "currency_exchange",
                "toolArgs", Map.of("from", "GBP", "to", "JPY", "amount", 100)));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("apiCallMade"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void calculatesConvertedAmountCorrectly() {
        Task task = taskWith(Map.of(
                "toolName", "currency_exchange",
                "toolArgs", Map.of("from", "USD", "to", "EUR", "amount", 500)));
        TaskResult result = worker.execute(task);

        Map<String, Object> convResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals(460.0, convResult.get("convertedAmount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void usesFixedTimestamp() {
        Task task = taskWith(Map.of(
                "toolName", "currency_exchange",
                "toolArgs", Map.of("from", "USD", "to", "EUR", "amount", 1000)));
        TaskResult result = worker.execute(task);

        Map<String, Object> convResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("2026-03-08T10:00:00Z", convResult.get("timestamp"));
    }

    @Test
    void handlesNullToolName() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", null);
        input.put("toolArgs", Map.of("from", "USD", "to", "EUR", "amount", 1000));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullToolArgs() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", "currency_exchange");
        input.put("toolArgs", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
        assertEquals(312, result.getOutputData().get("executionTimeMs"));
        assertEquals(true, result.getOutputData().get("apiCallMade"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
