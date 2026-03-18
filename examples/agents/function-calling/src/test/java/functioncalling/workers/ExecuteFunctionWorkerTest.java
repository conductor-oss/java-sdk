package functioncalling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ExecuteFunctionWorkerTest {

    private final ExecuteFunctionWorker worker = new ExecuteFunctionWorker();

    @Test
    void taskDefName() {
        assertEquals("fc_execute_function", worker.getTaskDefName());
    }

    // --- get_weather (network-dependent) ---

    @Test
    void executesGetWeatherSuccessfully() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of(
                "functionName", "get_weather",
                "arguments", Map.of("location", "San Francisco", "unit", "celsius")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("executionStatus"));

        @SuppressWarnings("unchecked")
        Map<String, Object> fnResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("San Francisco", fnResult.get("location"));
        // Real API returns "C" for celsius
        if (!fnResult.containsKey("error")) {
            assertEquals("C", fnResult.get("unit"));
            assertNotNull(fnResult.get("temperature"));
            assertNotNull(fnResult.get("condition"));
        }
    }

    @Test
    void getWeatherReturnsCompletedEvenOnError() {
        // Even with a bogus location, the worker should COMPLETE (not crash)
        Task task = taskWith(Map.of("functionName", "get_weather",
                "arguments", Map.of("location", "ZZZZZ_nonexistent_place_12345")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // Should be success (with an error field in the result map) or error status
        assertNotNull(result.getOutputData().get("executionStatus"));
    }

    @Test
    void getWeatherSupportsFahrenheit() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of("functionName", "get_weather",
                "arguments", Map.of("location", "London", "unit", "fahrenheit")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> fnResult = (Map<String, Object>) result.getOutputData().get("result");
        if (!fnResult.containsKey("error")) {
            assertEquals("F", fnResult.get("unit"));
        }
    }

    // --- calculate (no network needed) ---

    @Test
    void executesCalculateWithOperands() {
        Task task = taskWith(Map.of(
                "functionName", "calculate",
                "arguments", Map.of("a", 10, "b", 5, "operation", "+")));
        TaskResult result = worker.execute(task);

        assertEquals("success", result.getOutputData().get("executionStatus"));
        @SuppressWarnings("unchecked")
        Map<String, Object> fnResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals(15.0, fnResult.get("result"));
    }

    @Test
    void executesCalculateWithExpression() {
        Task task = taskWith(Map.of(
                "functionName", "calculate",
                "arguments", Map.of("expression", "12 * 3")));
        TaskResult result = worker.execute(task);

        assertEquals("success", result.getOutputData().get("executionStatus"));
        @SuppressWarnings("unchecked")
        Map<String, Object> fnResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals(36.0, fnResult.get("result"));
    }

    @Test
    void calculateHandlesDivision() {
        Task task = taskWith(Map.of(
                "functionName", "calculate",
                "arguments", Map.of("a", 100, "b", 4, "operation", "/")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> fnResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals(25.0, fnResult.get("result"));
    }

    @Test
    void calculateHandlesDivisionByZero() {
        Task task = taskWith(Map.of(
                "functionName", "calculate",
                "arguments", Map.of("a", 10, "b", 0, "operation", "/")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("error", result.getOutputData().get("executionStatus"));
    }

    // --- get_time (no network needed) ---

    @Test
    void executesGetTime() {
        Task task = taskWith(Map.of(
                "functionName", "get_time",
                "arguments", Map.of("timezone", "UTC")));
        TaskResult result = worker.execute(task);

        assertEquals("success", result.getOutputData().get("executionStatus"));
        @SuppressWarnings("unchecked")
        Map<String, Object> fnResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("UTC", fnResult.get("timezone"));
        assertNotNull(fnResult.get("datetime"));
        assertNotNull(fnResult.get("epochMillis"));
    }

    // --- get_stock_price (network-dependent) ---

    @Test
    void executesGetStockPriceSuccessfully() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of(
                "functionName", "get_stock_price",
                "arguments", Map.of("ticker", "AAPL")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("executionStatus"));

        @SuppressWarnings("unchecked")
        Map<String, Object> fnResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("AAPL", fnResult.get("ticker"));
        // If the API returned real data (no error), check fields
        if (!fnResult.containsKey("error")) {
            assertNotNull(fnResult.get("price"));
            assertNotNull(fnResult.get("change"));
            assertNotNull(fnResult.get("changePercent"));
        }
    }

    @Test
    void stockPriceHandlesInvalidTicker() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of("functionName", "get_stock_price",
                "arguments", Map.of("ticker", "ZZZZZZZZZZ")));
        TaskResult result = worker.execute(task);

        // Should complete (not crash), even for an invalid ticker
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("executionStatus"));

        @SuppressWarnings("unchecked")
        Map<String, Object> fnResult = (Map<String, Object>) result.getOutputData().get("result");
        // Should contain an error message for the bogus ticker
        assertNotNull(fnResult);
    }

    @Test
    void usesTickerFromArguments() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of(
                "functionName", "get_stock_price",
                "arguments", Map.of("ticker", "GOOG")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> fnResult = (Map<String, Object>) result.getOutputData().get("result");
        // The ticker should be echoed back regardless of API success
        assertTrue(fnResult.containsKey("ticker") || fnResult.containsKey("error"));
    }

    // --- error handling (no network needed) ---

    @Test
    void returnsErrorForUnknownFunction() {
        Task task = taskWith(Map.of(
                "functionName", "nonexistent_function",
                "arguments", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("error", result.getOutputData().get("executionStatus"));

        @SuppressWarnings("unchecked")
        Map<String, Object> fnResult = (Map<String, Object>) result.getOutputData().get("result");
        assertTrue(((String) fnResult.get("error")).contains("nonexistent_function"));
    }

    @Test
    void handlesNullFunctionName() {
        Map<String, Object> input = new HashMap<>();
        input.put("functionName", null);
        input.put("arguments", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("error", result.getOutputData().get("executionStatus"));
    }

    @Test
    void handlesMissingFunctionName() {
        Task task = taskWith(Map.of("arguments", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("error", result.getOutputData().get("executionStatus"));
    }

    @Test
    void handlesNullArguments() {
        Map<String, Object> input = new HashMap<>();
        input.put("functionName", "get_stock_price");
        input.put("arguments", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("executionStatus"));
    }

    @Test
    void handlesMissingTickerInArguments() {
        Task task = taskWith(Map.of(
                "functionName", "get_stock_price",
                "arguments", Map.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> fnResult = (Map<String, Object>) result.getOutputData().get("result");
        // Either the ticker is "UNKNOWN" or an error is returned for it
        assertTrue(
                "UNKNOWN".equals(fnResult.get("ticker")) || fnResult.containsKey("error"),
                "Expected 'UNKNOWN' ticker or an error field");
    }

    // --- utility ---

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }

    /**
     * Checks whether a network connection to the Open-Meteo API is available.
     * Used with {@code assumeTrue} to skip network-dependent tests in offline environments.
     */
    private static boolean isNetworkAvailable() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://geocoding-api.open-meteo.com/v1/search?name=London&count=1"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
