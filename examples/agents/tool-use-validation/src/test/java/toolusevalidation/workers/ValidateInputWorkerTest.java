package toolusevalidation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateInputWorkerTest {

    private final ValidateInputWorker worker = new ValidateInputWorker();

    @Test
    void taskDefName() {
        assertEquals("tv_validate_input", worker.getTaskDefName());
    }

    @Test
    void returnsIsValidTrue() {
        Task task = taskWith(Map.of(
                "toolName", "weather_api",
                "toolArgs", Map.of("city", "London", "country", "UK"),
                "schema", Map.of("type", "object")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("true", result.getOutputData().get("isValid"));
    }

    @Test
    void returnsValidatedArgsSameAsInput() {
        Map<String, Object> toolArgs = Map.of("city", "London", "country", "UK", "units", "metric");
        Task task = taskWith(Map.of("toolName", "weather_api", "toolArgs", toolArgs));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> validatedArgs = (Map<String, Object>) result.getOutputData().get("validatedArgs");
        assertEquals("London", validatedArgs.get("city"));
        assertEquals("UK", validatedArgs.get("country"));
        assertEquals("metric", validatedArgs.get("units"));
    }

    @Test
    void returnsFourChecksAllPassing() {
        Task task = taskWith(Map.of("toolName", "weather_api",
                "toolArgs", Map.of("city", "London")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) result.getOutputData().get("checks");
        assertNotNull(checks);
        assertEquals(4, checks.size());
        for (Map<String, Object> check : checks) {
            assertEquals("passed", check.get("status"));
        }
    }

    @Test
    void returnsEmptyValidationErrors() {
        Task task = taskWith(Map.of("toolName", "weather_api",
                "toolArgs", Map.of("city", "London")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Object> errors = (List<Object>) result.getOutputData().get("validationErrors");
        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void handlesNullToolArgs() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", "weather_api");
        input.put("toolArgs", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("true", result.getOutputData().get("isValid"));
    }

    @Test
    void handlesMissingToolName() {
        Task task = taskWith(Map.of("toolArgs", Map.of("city", "London")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("checks"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("isValid"));
        assertNotNull(result.getOutputData().get("validatedArgs"));
    }

    @Test
    void checksContainExpectedCheckNames() {
        Task task = taskWith(Map.of("toolName", "weather_api",
                "toolArgs", Map.of("city", "London")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) result.getOutputData().get("checks");
        assertEquals("required_fields", checks.get(0).get("check"));
        assertEquals("type_validation", checks.get(1).get("check"));
        assertEquals("enum_validation", checks.get(2).get("check"));
        assertEquals("array_items", checks.get(3).get("check"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
