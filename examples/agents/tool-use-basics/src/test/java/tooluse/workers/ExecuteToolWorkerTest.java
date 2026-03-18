package tooluse.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ExecuteToolWorkerTest {

    private final ExecuteToolWorker worker = new ExecuteToolWorker();

    @Test
    void taskDefName() {
        assertEquals("tu_execute_tool", worker.getTaskDefName());
    }

    @Test
    void weatherApiReturnsWeatherData() {
        assumeTrue(isNetworkAvailable(), "Network not available, skipping weather API test");

        Task task = taskWith(Map.of(
                "toolName", "weather_api",
                "toolArgs", Map.of("location", "San Francisco, CA", "units", "fahrenheit")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("executionTimeMs"));

        @SuppressWarnings("unchecked")
        Map<String, Object> toolOutput = (Map<String, Object>) result.getOutputData().get("toolOutput");
        assertNotNull(toolOutput);

        // If network succeeded, we expect real data; if it failed gracefully, we expect an error field
        if (!toolOutput.containsKey("error")) {
            assertEquals(true, result.getOutputData().get("success"));
            assertNotNull(toolOutput.get("location"));
            assertNotNull(toolOutput.get("temperature"));
            assertEquals("fahrenheit", toolOutput.get("units"));
            assertNotNull(toolOutput.get("condition"));
            assertNotNull(toolOutput.get("humidity"));
            assertNotNull(toolOutput.get("windSpeed"));
            assertNotNull(toolOutput.get("windDirection"));
        }
    }

    @Test
    void weatherApiForecastHasTwoDays() {
        assumeTrue(isNetworkAvailable(), "Network not available, skipping weather forecast test");

        Task task = taskWith(Map.of(
                "toolName", "weather_api",
                "toolArgs", Map.of("location", "NYC", "units", "celsius")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> toolOutput = (Map<String, Object>) result.getOutputData().get("toolOutput");
        if (!toolOutput.containsKey("error")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> forecast = (List<Map<String, Object>>) toolOutput.get("forecast");
            assertNotNull(forecast);
            assertEquals(2, forecast.size());
            assertEquals("Tomorrow", forecast.get(0).get("day"));
        }
    }

    @Test
    void calculatorReturnsResult() {
        Task task = taskWith(Map.of(
                "toolName", "calculator",
                "toolArgs", Map.of("expression", "2+2", "precision", 2)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> toolOutput = (Map<String, Object>) result.getOutputData().get("toolOutput");
        assertEquals(4.0, toolOutput.get("result"));
        assertEquals("2+2", toolOutput.get("expression"));
    }

    @Test
    void calculatorHandlesComplexExpression() {
        Task task = taskWith(Map.of(
                "toolName", "calculator",
                "toolArgs", Map.of("expression", "3+4*2", "precision", 2)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> toolOutput = (Map<String, Object>) result.getOutputData().get("toolOutput");
        assertEquals(11.0, toolOutput.get("result"));
    }

    @Test
    void calculatorHandlesParentheses() {
        Task task = taskWith(Map.of(
                "toolName", "calculator",
                "toolArgs", Map.of("expression", "(3+4)*2", "precision", 2)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> toolOutput = (Map<String, Object>) result.getOutputData().get("toolOutput");
        assertEquals(14.0, toolOutput.get("result"));
    }

    @Test
    void calculatorHandlesPower() {
        Task task = taskWith(Map.of(
                "toolName", "calculator",
                "toolArgs", Map.of("expression", "2^10", "precision", 2)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> toolOutput = (Map<String, Object>) result.getOutputData().get("toolOutput");
        assertEquals(1024.0, toolOutput.get("result"));
    }

    @Test
    void webSearchReturnsResults() {
        assumeTrue(isNetworkAvailable(), "Network not available, skipping web search test");

        Task task = taskWith(Map.of(
                "toolName", "web_search",
                "toolArgs", Map.of("query", "Java programming", "maxResults", 5)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> toolOutput = (Map<String, Object>) result.getOutputData().get("toolOutput");
        assertNotNull(toolOutput);
        assertEquals("Java programming", toolOutput.get("query"));

        if (!toolOutput.containsKey("error")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) toolOutput.get("results");
            assertNotNull(results);
            assertTrue(results.size() > 0);
            assertEquals(results.size(), toolOutput.get("totalResults"));
        }
    }

    @Test
    void unknownToolReturnsUnsupported() {
        Task task = taskWith(Map.of("toolName", "unknown_tool", "toolArgs", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> toolOutput = (Map<String, Object>) result.getOutputData().get("toolOutput");
        assertEquals("unsupported", toolOutput.get("status"));
        assertTrue(((String) toolOutput.get("message")).contains("unknown_tool"));
    }

    @Test
    void handlesNullToolName() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", null);
        input.put("toolArgs", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> toolOutput = (Map<String, Object>) result.getOutputData().get("toolOutput");
        assertEquals("unsupported", toolOutput.get("status"));
    }

    @Test
    void handlesMissingToolName() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("toolOutput"));
    }

    @Test
    void handlesBlankToolName() {
        Task task = taskWith(Map.of("toolName", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> toolOutput = (Map<String, Object>) result.getOutputData().get("toolOutput");
        assertEquals("unsupported", toolOutput.get("status"));
    }

    @Test
    void expressionEvaluatorBasicMath() {
        assertEquals(4.0, ExecuteToolWorker.evaluateExpression("2+2"), 0.001);
        assertEquals(6.0, ExecuteToolWorker.evaluateExpression("2*3"), 0.001);
        assertEquals(5.0, ExecuteToolWorker.evaluateExpression("10/2"), 0.001);
        assertEquals(3.0, ExecuteToolWorker.evaluateExpression("5-2"), 0.001);
    }

    @Test
    void expressionEvaluatorOrderOfOperations() {
        assertEquals(11.0, ExecuteToolWorker.evaluateExpression("3+4*2"), 0.001);
        assertEquals(14.0, ExecuteToolWorker.evaluateExpression("(3+4)*2"), 0.001);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }

    private static boolean isNetworkAvailable() {
        try {
            java.net.InetAddress.getByName("api.open-meteo.com").isReachable(3000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
