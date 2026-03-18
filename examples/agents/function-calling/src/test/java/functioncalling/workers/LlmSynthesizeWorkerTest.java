package functioncalling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LlmSynthesizeWorkerTest {

    private final LlmSynthesizeWorker worker = new LlmSynthesizeWorker();

    @Test
    void taskDefName() {
        assertEquals("fc_llm_synthesize", worker.getTaskDefName());
    }

    @Test
    void synthesizesNaturalLanguageAnswer() {
        Map<String, Object> functionResult = Map.of(
                "ticker", "AAPL",
                "companyName", "Apple Inc.",
                "price", 189.45,
                "currency", "USD",
                "change", 2.35,
                "changePercent", 1.26);
        Task task = taskWith(Map.of(
                "userQuery", "What's the current price of Apple stock?",
                "functionName", "get_stock_price",
                "functionResult", functionResult));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("Apple Inc."));
        assertTrue(answer.contains("189.45"));
    }

    @Test
    void answerIncludesChangeData() {
        Map<String, Object> functionResult = Map.of(
                "ticker", "AAPL",
                "companyName", "Apple Inc.",
                "price", 189.45,
                "currency", "USD",
                "change", 2.35,
                "changePercent", 1.26);
        Task task = taskWith(Map.of(
                "userQuery", "Apple stock price",
                "functionName", "get_stock_price",
                "functionResult", functionResult));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("2.35"));
        assertTrue(answer.contains("1.26"));
    }

    @Test
    void returnsConfidenceScore() {
        Map<String, Object> functionResult = Map.of(
                "ticker", "AAPL", "price", 189.45, "currency", "USD",
                "change", 2.35, "changePercent", 1.26);
        Task task = taskWith(Map.of(
                "userQuery", "Stock price",
                "functionName", "get_stock_price",
                "functionResult", functionResult));
        TaskResult result = worker.execute(task);

        assertEquals(0.97, result.getOutputData().get("confidence"));
    }

    @Test
    void returnsSourceFunctionUsed() {
        Map<String, Object> functionResult = Map.of(
                "ticker", "AAPL", "price", 189.45, "currency", "USD",
                "change", 2.35, "changePercent", 1.26);
        Task task = taskWith(Map.of(
                "userQuery", "Price check",
                "functionName", "get_stock_price",
                "functionResult", functionResult));
        TaskResult result = worker.execute(task);

        assertEquals("get_stock_price", result.getOutputData().get("sourceFunctionUsed"));
    }

    @Test
    void answerMentionsFunctionName() {
        Map<String, Object> functionResult = Map.of(
                "ticker", "MSFT", "companyName", "Microsoft Corp.",
                "price", 420.10, "currency", "USD",
                "change", 5.20, "changePercent", 1.25);
        Task task = taskWith(Map.of(
                "userQuery", "Microsoft stock",
                "functionName", "get_stock_price",
                "functionResult", functionResult));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("get_stock_price"));
    }

    @Test
    void handlesMissingPriceInResult() {
        Map<String, Object> functionResult = Map.of("error", "Service unavailable");
        Task task = taskWith(Map.of(
                "userQuery", "Apple stock",
                "functionName", "get_stock_price",
                "functionResult", functionResult));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("unable to retrieve"));
    }

    @Test
    void handlesNullFunctionResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("userQuery", "Apple stock");
        input.put("functionName", "get_stock_price");
        input.put("functionResult", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("unable to retrieve"));
    }

    @Test
    void handlesNullUserQuery() {
        Map<String, Object> input = new HashMap<>();
        input.put("userQuery", null);
        input.put("functionName", "get_stock_price");
        input.put("functionResult", Map.of("price", 100.0, "currency", "USD",
                "change", 1.0, "changePercent", 0.5));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
