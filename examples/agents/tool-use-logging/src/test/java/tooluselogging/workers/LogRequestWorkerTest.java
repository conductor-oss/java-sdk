package tooluselogging.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogRequestWorkerTest {

    private final LogRequestWorker worker = new LogRequestWorker();

    @Test
    void taskDefName() {
        assertEquals("tl_log_request", worker.getTaskDefName());
    }

    @Test
    void returnsFixedRequestId() {
        Task task = taskWith(Map.of(
                "toolName", "sentiment_analysis",
                "toolArgs", Map.of("text", "hello"),
                "userId", "user-7392",
                "sessionId", "sess-abc-12345"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("req-fixed-abc123", result.getOutputData().get("requestId"));
    }

    @Test
    void returnsFixedTimestamp() {
        Task task = taskWith(Map.of("toolName", "test_tool", "userId", "user-1"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-08T10:00:00Z", result.getOutputData().get("timestamp"));
    }

    @Test
    void returnsLoggedTrue() {
        Task task = taskWith(Map.of("toolName", "test_tool", "userId", "user-1"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void returnsLogEntryWithCorrectFields() {
        Task task = taskWith(Map.of(
                "toolName", "sentiment_analysis",
                "userId", "user-7392",
                "sessionId", "sess-abc-12345"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> logEntry = (Map<String, Object>) result.getOutputData().get("logEntry");
        assertNotNull(logEntry);
        assertEquals("req-fixed-abc123", logEntry.get("requestId"));
        assertEquals("sentiment_analysis", logEntry.get("toolName"));
        assertEquals("user-7392", logEntry.get("userId"));
        assertEquals("sess-abc-12345", logEntry.get("sessionId"));
        assertEquals("request", logEntry.get("direction"));
    }

    @Test
    void handlesNullToolName() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", null);
        input.put("userId", "user-1");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> logEntry = (Map<String, Object>) result.getOutputData().get("logEntry");
        assertEquals("unknown_tool", logEntry.get("toolName"));
    }

    @Test
    void handlesNullUserId() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", "test_tool");
        input.put("userId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> logEntry = (Map<String, Object>) result.getOutputData().get("logEntry");
        assertEquals("anonymous", logEntry.get("userId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("requestId"));
        assertNotNull(result.getOutputData().get("logEntry"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
