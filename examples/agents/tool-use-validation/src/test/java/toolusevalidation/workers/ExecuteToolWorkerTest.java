package toolusevalidation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteToolWorkerTest {

    private final ExecuteToolWorker worker = new ExecuteToolWorker();

    @Test
    void taskDefName() {
        assertEquals("tv_execute_tool", worker.getTaskDefName());
    }

    @Test
    void returnsRawOutputWithWeatherData() {
        Task task = taskWith(Map.of("toolName", "weather_api",
                "validatedArgs", Map.of("city", "London")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> rawOutput = (Map<String, Object>) result.getOutputData().get("rawOutput");
        assertNotNull(rawOutput);
        assertEquals(14.2, rawOutput.get("temperature"));
        assertEquals(72, rawOutput.get("humidity"));
        assertEquals("partly cloudy", rawOutput.get("conditions"));
    }

    @Test
    void returnsWindData() {
        Task task = taskWith(Map.of("toolName", "weather_api",
                "validatedArgs", Map.of("city", "London")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> rawOutput = (Map<String, Object>) result.getOutputData().get("rawOutput");
        assertEquals(18.5, rawOutput.get("windSpeed"));
        assertEquals("SW", rawOutput.get("windDirection"));
    }

    @Test
    void returnsThreeDayForecast() {
        Task task = taskWith(Map.of("toolName", "weather_api",
                "validatedArgs", Map.of("city", "London")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> rawOutput = (Map<String, Object>) result.getOutputData().get("rawOutput");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> forecast = (List<Map<String, Object>>) rawOutput.get("forecast");
        assertNotNull(forecast);
        assertEquals(3, forecast.size());
        assertEquals("Monday", forecast.get(0).get("day"));
        assertEquals("Tuesday", forecast.get(1).get("day"));
        assertEquals("Wednesday", forecast.get(2).get("day"));
    }

    @Test
    void returnsFixedTimestamp() {
        Task task = taskWith(Map.of("toolName", "weather_api",
                "validatedArgs", Map.of("city", "London")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> rawOutput = (Map<String, Object>) result.getOutputData().get("rawOutput");
        assertEquals("2026-03-08T10:00:00Z", rawOutput.get("timestamp"));
        assertEquals("weather_api_v3", rawOutput.get("provider"));
    }

    @Test
    void returnsExecutionTimeMs() {
        Task task = taskWith(Map.of("toolName", "weather_api",
                "validatedArgs", Map.of("city", "London")));
        TaskResult result = worker.execute(task);

        assertEquals(245, result.getOutputData().get("executionTimeMs"));
    }

    @Test
    void handlesMissingToolName() {
        Task task = taskWith(Map.of("validatedArgs", Map.of("city", "London")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("rawOutput"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("rawOutput"));
        assertNotNull(result.getOutputData().get("executionTimeMs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
