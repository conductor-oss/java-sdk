package tooluseerror.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TryFallbackToolWorkerTest {

    private final TryFallbackToolWorker worker = new TryFallbackToolWorker();

    @Test
    void taskDefName() {
        assertEquals("te_try_fallback_tool", worker.getTaskDefName());
    }

    @Test
    void returnsSuccessStatus() {
        Task task = taskWith(Map.of(
                "query", "What are the coordinates of San Francisco?",
                "toolName", "openstreetmap_nominatim",
                "primaryError", Map.of("message", "Service temporarily unavailable")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("toolStatus"));
    }

    @Test
    void returnsGeocodingResult() {
        Task task = taskWith(Map.of(
                "query", "coordinates of SF",
                "toolName", "openstreetmap_nominatim",
                "primaryError", Map.of("message", "Service temporarily unavailable")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> geocoding = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(geocoding);
        assertEquals("San Francisco, CA", geocoding.get("location"));
        assertEquals(0.97, geocoding.get("confidence"));
        assertEquals("openstreetmap_nominatim", geocoding.get("provider"));

        @SuppressWarnings("unchecked")
        Map<String, Object> coords = (Map<String, Object>) geocoding.get("coordinates");
        assertEquals(37.7899, coords.get("lat"));
        assertEquals(-122.4194, coords.get("lng"));
    }

    @Test
    void extractsFallbackReasonFromPrimaryError() {
        Task task = taskWith(Map.of(
                "query", "test",
                "toolName", "fallback_api",
                "primaryError", Map.of("message", "Rate limit exceeded")));
        TaskResult result = worker.execute(task);

        assertEquals("Rate limit exceeded", result.getOutputData().get("fallbackReason"));
    }

    @Test
    void handlesNullPrimaryError() {
        Map<String, Object> input = new HashMap<>();
        input.put("query", "test");
        input.put("toolName", "fallback_api");
        input.put("primaryError", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown error", result.getOutputData().get("fallbackReason"));
    }

    @Test
    void handlesNullToolName() {
        Map<String, Object> input = new HashMap<>();
        input.put("query", "test");
        input.put("toolName", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> geocoding = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("unknown_tool", geocoding.get("provider"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("toolStatus"));
    }

    @Test
    void handlesMissingMessageInPrimaryError() {
        Map<String, Object> errorWithoutMessage = new HashMap<>();
        errorWithoutMessage.put("code", 500);
        errorWithoutMessage.put("message", null);
        Task task = taskWith(Map.of(
                "query", "test",
                "toolName", "fallback_api",
                "primaryError", errorWithoutMessage));
        TaskResult result = worker.execute(task);

        assertEquals("unknown error", result.getOutputData().get("fallbackReason"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
