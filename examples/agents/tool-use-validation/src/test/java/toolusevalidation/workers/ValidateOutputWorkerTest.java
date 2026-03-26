package toolusevalidation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateOutputWorkerTest {

    private final ValidateOutputWorker worker = new ValidateOutputWorker();

    @Test
    void taskDefName() {
        assertEquals("tv_validate_output", worker.getTaskDefName());
    }

    @Test
    void returnsIsValidTrue() {
        Task task = taskWith(Map.of(
                "toolName", "weather_api",
                "rawOutput", Map.of("temperature", 14.2, "humidity", 72, "conditions", "partly cloudy"),
                "outputSchema", Map.of("type", "object")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("true", result.getOutputData().get("isValid"));
    }

    @Test
    void returnsValidatedOutputSameAsRawOutput() {
        Map<String, Object> rawOutput = Map.of("temperature", 14.2, "humidity", 72, "conditions", "partly cloudy");
        Task task = taskWith(Map.of("toolName", "weather_api", "rawOutput", rawOutput));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> validatedOutput = (Map<String, Object>) result.getOutputData().get("validatedOutput");
        assertEquals(14.2, validatedOutput.get("temperature"));
        assertEquals(72, validatedOutput.get("humidity"));
        assertEquals("partly cloudy", validatedOutput.get("conditions"));
    }

    @Test
    void returnsValidationReportWithFourChecks() {
        Task task = taskWith(Map.of("toolName", "weather_api",
                "rawOutput", Map.of("temperature", 14.2)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("validationReport");
        assertNotNull(report);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) report.get("checks");
        assertEquals(4, checks.size());
        for (Map<String, Object> check : checks) {
            assertEquals("passed", check.get("status"));
        }
    }

    @Test
    void returnsValidationReportWithFixedTimestamp() {
        Task task = taskWith(Map.of("toolName", "weather_api",
                "rawOutput", Map.of("temperature", 14.2)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("validationReport");
        assertEquals("2026-03-08T10:00:01Z", report.get("timestamp"));

        @SuppressWarnings("unchecked")
        List<Object> errors = (List<Object>) report.get("errors");
        assertTrue(errors.isEmpty());
    }

    @Test
    void handlesNullRawOutput() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", "weather_api");
        input.put("rawOutput", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("true", result.getOutputData().get("isValid"));
    }

    @Test
    void handlesMissingToolName() {
        Task task = taskWith(Map.of("rawOutput", Map.of("temperature", 14.2)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("validationReport"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("isValid"));
        assertNotNull(result.getOutputData().get("validatedOutput"));
        assertNotNull(result.getOutputData().get("validationReport"));
    }

    @Test
    void validationReportChecksHaveExpectedNames() {
        Task task = taskWith(Map.of("toolName", "weather_api",
                "rawOutput", Map.of("temperature", 14.2)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("validationReport");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) report.get("checks");
        assertEquals("required_fields", checks.get(0).get("check"));
        assertEquals("type_validation", checks.get(1).get("check"));
        assertEquals("range_validation", checks.get(2).get("check"));
        assertEquals("format_validation", checks.get(3).get("check"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
