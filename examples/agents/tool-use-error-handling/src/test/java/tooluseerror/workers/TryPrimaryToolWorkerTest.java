package tooluseerror.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TryPrimaryToolWorkerTest {

    private final TryPrimaryToolWorker worker = new TryPrimaryToolWorker();

    @Test
    void taskDefName() {
        assertEquals("te_try_primary_tool", worker.getTaskDefName());
    }

    @Test
    void returnsFailureStatus() {
        Task task = taskWith(Map.of("query", "What are the coordinates of San Francisco?",
                "toolName", "google_geocoding_api"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("failure", result.getOutputData().get("toolStatus"));
    }

    @Test
    void returnsErrorWithCode503() {
        Task task = taskWith(Map.of("query", "What are the coordinates of San Francisco?",
                "toolName", "google_geocoding_api"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) result.getOutputData().get("error");
        assertNotNull(error);
        assertEquals(503, error.get("code"));
        assertEquals("Service temporarily unavailable", error.get("message"));
        assertEquals("google_geocoding_api", error.get("tool"));
    }

    @Test
    void returnsNullResult() {
        Task task = taskWith(Map.of("query", "test query", "toolName", "some_tool"));
        TaskResult result = worker.execute(task);

        assertNull(result.getOutputData().get("result"));
    }

    @Test
    void returnsFixedTimestamp() {
        Task task = taskWith(Map.of("query", "test query", "toolName", "some_tool"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-08T10:00:00Z", result.getOutputData().get("attemptedAt"));
    }

    @Test
    void handlesNullToolName() {
        Map<String, Object> input = new HashMap<>();
        input.put("query", "test query");
        input.put("toolName", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) result.getOutputData().get("error");
        assertEquals("unknown_tool", error.get("tool"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("failure", result.getOutputData().get("toolStatus"));
    }

    @Test
    void handlesBlankQuery() {
        Task task = taskWith(Map.of("query", "  ", "toolName", "api"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("failure", result.getOutputData().get("toolStatus"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
