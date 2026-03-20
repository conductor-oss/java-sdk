package toolusevalidation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeliverWorkerTest {

    private final DeliverWorker worker = new DeliverWorker();

    @Test
    void taskDefName() {
        assertEquals("tv_deliver", worker.getTaskDefName());
    }

    @Test
    void returnsFormattedResultWithWeatherSummary() {
        Task task = taskWith(Map.of(
                "userRequest", "What is the current weather in London?",
                "validatedOutput", Map.of(
                        "temperature", 14.2, "humidity", 72,
                        "conditions", "partly cloudy",
                        "windSpeed", 18.5, "windDirection", "SW"),
                "validationReport", Map.of("checks", java.util.List.of())));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String formatted = (String) result.getOutputData().get("formattedResult");
        assertNotNull(formatted);
        assertTrue(formatted.contains("London"));
        assertTrue(formatted.contains("14.2"));
        assertTrue(formatted.contains("partly cloudy"));
    }

    @Test
    void formattedResultContainsWindInfo() {
        Task task = taskWith(Map.of(
                "userRequest", "Weather?",
                "validatedOutput", Map.of(
                        "temperature", 14.2, "humidity", 72,
                        "conditions", "partly cloudy",
                        "windSpeed", 18.5, "windDirection", "SW")));
        TaskResult result = worker.execute(task);

        String formatted = (String) result.getOutputData().get("formattedResult");
        assertTrue(formatted.contains("18.5"));
        assertTrue(formatted.contains("SW"));
    }

    @Test
    void returnsValidationSummary() {
        Task task = taskWith(Map.of(
                "userRequest", "Weather?",
                "validatedOutput", Map.of("temperature", 14.2, "conditions", "cloudy")));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("validationSummary");
        assertNotNull(summary);
        assertTrue(summary.contains("passed"));
        assertTrue(summary.contains("zero errors"));
    }

    @Test
    void formattedResultContainsForecastMention() {
        Task task = taskWith(Map.of(
                "userRequest", "Weather?",
                "validatedOutput", Map.of(
                        "temperature", 14.2, "humidity", 72,
                        "conditions", "partly cloudy",
                        "windSpeed", 18.5, "windDirection", "SW")));
        TaskResult result = worker.execute(task);

        String formatted = (String) result.getOutputData().get("formattedResult");
        assertTrue(formatted.contains("forecast"));
    }

    @Test
    void handlesNullUserRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("userRequest", null);
        input.put("validatedOutput", Map.of("temperature", 14.2));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("formattedResult"));
    }

    @Test
    void handlesNullValidatedOutput() {
        Map<String, Object> input = new HashMap<>();
        input.put("userRequest", "Weather?");
        input.put("validatedOutput", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("formattedResult"));
        assertNotNull(result.getOutputData().get("validationSummary"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("formattedResult"));
        assertNotNull(result.getOutputData().get("validationSummary"));
    }

    @Test
    void handlesEmptyValidatedOutput() {
        Task task = taskWith(Map.of(
                "userRequest", "Weather?",
                "validatedOutput", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String formatted = (String) result.getOutputData().get("formattedResult");
        assertTrue(formatted.contains("N/A"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
