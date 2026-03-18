package functioncalling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LlmPlanWorkerTest {

    private final LlmPlanWorker worker = new LlmPlanWorker();

    @Test
    void taskDefName() {
        assertEquals("fc_llm_plan", worker.getTaskDefName());
    }

    @Test
    void returnsLlmResponseWithFunctionCall() {
        Task task = taskWith(Map.of(
                "userQuery", "What's the current price of Apple stock?",
                "functionDefinitions", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> llmResponse = (Map<String, Object>) result.getOutputData().get("llmResponse");
        assertNotNull(llmResponse);
        assertTrue(llmResponse.containsKey("reasoning"));
        assertTrue(llmResponse.containsKey("functionCall"));
    }

    @Test
    void stockQueryCallsGetStockPrice() {
        Task task = taskWith(Map.of("userQuery", "Get AAPL price"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> llmResponse = (Map<String, Object>) result.getOutputData().get("llmResponse");
        @SuppressWarnings("unchecked")
        Map<String, Object> functionCall = (Map<String, Object>) llmResponse.get("functionCall");

        assertEquals("get_stock_price", functionCall.get("name"));
        assertNotNull(functionCall.get("arguments"));
    }

    @Test
    void stockQueryExtractsTicker() {
        Task task = taskWith(Map.of("userQuery", "Apple stock price"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> llmResponse = (Map<String, Object>) result.getOutputData().get("llmResponse");
        @SuppressWarnings("unchecked")
        Map<String, Object> functionCall = (Map<String, Object>) llmResponse.get("functionCall");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) functionCall.get("arguments");

        assertEquals("AAPL", arguments.get("ticker"));
        assertEquals(true, arguments.get("includeChange"));
    }

    @Test
    void stockReasoningContainsExplanation() {
        Task task = taskWith(Map.of("userQuery", "Show me Apple stock"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> llmResponse = (Map<String, Object>) result.getOutputData().get("llmResponse");
        String reasoning = (String) llmResponse.get("reasoning");

        assertNotNull(reasoning);
        assertFalse(reasoning.isBlank());
        assertTrue(reasoning.contains("get_stock_price"));
    }

    @Test
    void weatherQueryCallsGetWeather() {
        Task task = taskWith(Map.of("userQuery", "What's the weather in Seattle?"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> llmResponse = (Map<String, Object>) result.getOutputData().get("llmResponse");
        @SuppressWarnings("unchecked")
        Map<String, Object> functionCall = (Map<String, Object>) llmResponse.get("functionCall");

        assertEquals("get_weather", functionCall.get("name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) functionCall.get("arguments");
        assertNotNull(args.get("location"));
    }

    @Test
    void mathQueryCallsCalculate() {
        Task task = taskWith(Map.of("userQuery", "Calculate 2 + 3 * 4"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> llmResponse = (Map<String, Object>) result.getOutputData().get("llmResponse");
        @SuppressWarnings("unchecked")
        Map<String, Object> functionCall = (Map<String, Object>) llmResponse.get("functionCall");

        assertEquals("calculate", functionCall.get("name"));
    }

    @Test
    void timeQueryCallsGetTime() {
        Task task = taskWith(Map.of("userQuery", "What time is it?"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> llmResponse = (Map<String, Object>) result.getOutputData().get("llmResponse");
        @SuppressWarnings("unchecked")
        Map<String, Object> functionCall = (Map<String, Object>) llmResponse.get("functionCall");

        assertEquals("get_time", functionCall.get("name"));
    }

    @Test
    void unknownQueryFallsBackToGeneral() {
        Task task = taskWith(Map.of("userQuery", "Tell me a joke"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> llmResponse = (Map<String, Object>) result.getOutputData().get("llmResponse");
        @SuppressWarnings("unchecked")
        Map<String, Object> functionCall = (Map<String, Object>) llmResponse.get("functionCall");

        assertEquals("general_query", functionCall.get("name"));
    }

    @Test
    void handlesNullUserQuery() {
        Map<String, Object> input = new HashMap<>();
        input.put("userQuery", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("llmResponse"));
    }

    @Test
    void handlesMissingUserQuery() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("llmResponse"));
    }

    @Test
    void handlesBlankUserQuery() {
        Task task = taskWith(Map.of("userQuery", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("llmResponse"));
    }

    @Test
    void weatherQueryWithCelsius() {
        Task task = taskWith(Map.of("userQuery", "Weather in London in celsius"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> llmResponse = (Map<String, Object>) result.getOutputData().get("llmResponse");
        @SuppressWarnings("unchecked")
        Map<String, Object> functionCall = (Map<String, Object>) llmResponse.get("functionCall");
        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) functionCall.get("arguments");

        assertEquals("get_weather", functionCall.get("name"));
        assertEquals("celsius", args.get("units"));
    }

    @Test
    void stockQueryRecognizesCompanyNames() {
        Task task = taskWith(Map.of("userQuery", "What is the stock price of Tesla?"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> llmResponse = (Map<String, Object>) result.getOutputData().get("llmResponse");
        @SuppressWarnings("unchecked")
        Map<String, Object> functionCall = (Map<String, Object>) llmResponse.get("functionCall");
        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) functionCall.get("arguments");

        assertEquals("get_stock_price", functionCall.get("name"));
        assertEquals("TSLA", args.get("ticker"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
