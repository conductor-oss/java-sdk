package tooluse.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatResultWorkerTest {

    private final FormatResultWorker worker = new FormatResultWorker();

    @Test
    void taskDefName() {
        assertEquals("tu_format_result", worker.getTaskDefName());
    }

    @Test
    void formatsWeatherApiResult() {
        Map<String, Object> toolOutput = new HashMap<>(Map.of(
                "location", "San Francisco, CA",
                "temperature", 62,
                "units", "fahrenheit",
                "condition", "Partly Cloudy",
                "humidity", 72,
                "windSpeed", 12,
                "windDirection", "W"
        ));

        Task task = taskWith(Map.of(
                "userRequest", "What's the weather in SF?",
                "toolName", "weather_api",
                "toolOutput", toolOutput));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("San Francisco, CA"));
        assertTrue(answer.contains("62"));
        assertTrue(answer.contains("Partly Cloudy"));
        assertTrue(answer.contains("72%"));

        assertEquals("weather_api", result.getOutputData().get("toolUsed"));
        assertNotNull(result.getOutputData().get("sourceData"));
    }

    @Test
    void formatsCalculatorResult() {
        Map<String, Object> toolOutput = new HashMap<>(Map.of(
                "expression", "5+10",
                "result", 15,
                "precision", 2
        ));

        Task task = taskWith(Map.of(
                "userRequest", "Calculate 5+10",
                "toolName", "calculator",
                "toolOutput", toolOutput));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("15"));
        assertEquals("calculator", result.getOutputData().get("toolUsed"));
    }

    @Test
    void formatsWebSearchResult() {
        Map<String, Object> toolOutput = new HashMap<>(Map.of(
                "query", "AI news",
                "totalResults", 5
        ));

        Task task = taskWith(Map.of(
                "userRequest", "Search for AI news",
                "toolName", "web_search",
                "toolOutput", toolOutput));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("5"));
        assertTrue(answer.contains("search results"));
    }

    @Test
    void formatsUnknownToolResult() {
        Task task = taskWith(Map.of(
                "userRequest", "Do something",
                "toolName", "custom_tool",
                "toolOutput", Map.of("data", "value")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("Do something"));
        assertTrue(answer.contains("custom_tool"));
    }

    @Test
    void weatherAnswerContainsUnitSymbol() {
        Map<String, Object> toolOutput = new HashMap<>(Map.of(
                "location", "NYC",
                "temperature", 45,
                "units", "celsius",
                "condition", "Rainy",
                "humidity", 90,
                "windSpeed", 20,
                "windDirection", "NE"
        ));

        Task task = taskWith(Map.of(
                "userRequest", "Weather in NYC",
                "toolName", "weather_api",
                "toolOutput", toolOutput));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("C"));
        assertTrue(answer.contains("NE"));
    }

    @Test
    void handlesNullUserRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("userRequest", null);
        input.put("toolName", "weather_api");
        input.put("toolOutput", Map.of("location", "LA", "temperature", 75,
                "units", "fahrenheit", "condition", "Sunny",
                "humidity", 40, "windSpeed", 5, "windDirection", "S"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void handlesMissingToolName() {
        Task task = taskWith(Map.of("userRequest", "Hello"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("unknown"));
    }

    @Test
    void handlesBlankToolName() {
        Task task = taskWith(Map.of("userRequest", "test", "toolName", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("unknown"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
