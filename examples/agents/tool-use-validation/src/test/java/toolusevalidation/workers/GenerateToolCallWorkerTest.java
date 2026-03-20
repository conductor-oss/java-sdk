package toolusevalidation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateToolCallWorkerTest {

    private final GenerateToolCallWorker worker = new GenerateToolCallWorker();

    @Test
    void taskDefName() {
        assertEquals("tv_generate_tool_call", worker.getTaskDefName());
    }

    @Test
    void returnsToolArgs() {
        Task task = taskWith(Map.of("userRequest", "What is the current weather in London?",
                "toolName", "weather_api"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> toolArgs = (Map<String, Object>) result.getOutputData().get("toolArgs");
        assertNotNull(toolArgs);
        assertEquals("London", toolArgs.get("city"));
        assertEquals("UK", toolArgs.get("country"));
        assertEquals("metric", toolArgs.get("units"));
    }

    @Test
    void returnsToolArgsWithIncludeArray() {
        Task task = taskWith(Map.of("userRequest", "Weather in London?", "toolName", "weather_api"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> toolArgs = (Map<String, Object>) result.getOutputData().get("toolArgs");
        @SuppressWarnings("unchecked")
        List<String> include = (List<String>) toolArgs.get("include");
        assertNotNull(include);
        assertEquals(4, include.size());
        assertTrue(include.contains("temperature"));
        assertTrue(include.contains("humidity"));
        assertTrue(include.contains("wind"));
        assertTrue(include.contains("forecast"));
    }

    @Test
    void returnsInputSchema() {
        Task task = taskWith(Map.of("userRequest", "Weather?", "toolName", "weather_api"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) result.getOutputData().get("inputSchema");
        assertNotNull(inputSchema);
        assertEquals("object", inputSchema.get("type"));

        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) inputSchema.get("required");
        assertTrue(required.contains("city"));
        assertTrue(required.contains("country"));
    }

    @Test
    void returnsOutputSchema() {
        Task task = taskWith(Map.of("userRequest", "Weather?", "toolName", "weather_api"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> outputSchema = (Map<String, Object>) result.getOutputData().get("outputSchema");
        assertNotNull(outputSchema);
        assertEquals("object", outputSchema.get("type"));

        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) outputSchema.get("required");
        assertTrue(required.contains("temperature"));
        assertTrue(required.contains("humidity"));
        assertTrue(required.contains("conditions"));
    }

    @Test
    void handlesNullUserRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("userRequest", null);
        input.put("toolName", "weather_api");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("toolArgs"));
    }

    @Test
    void handlesMissingToolName() {
        Task task = taskWith(Map.of("userRequest", "Weather?"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("toolArgs"));
        assertNotNull(result.getOutputData().get("inputSchema"));
        assertNotNull(result.getOutputData().get("outputSchema"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("toolArgs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
