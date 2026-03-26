package tooluselogging.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogResponseWorkerTest {

    private final LogResponseWorker worker = new LogResponseWorker();

    @Test
    void taskDefName() {
        assertEquals("tl_log_response", worker.getTaskDefName());
    }

    @Test
    void returnsFixedTimestamp() {
        Task task = taskWith(Map.of(
                "requestId", "req-fixed-abc123",
                "toolName", "sentiment_analysis",
                "result", Map.of("sentiment", "positive"),
                "executionTimeMs", 187,
                "toolStatus", "success"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("2026-03-08T10:00:01Z", result.getOutputData().get("timestamp"));
    }

    @Test
    void returnsRequestId() {
        Task task = taskWith(Map.of(
                "requestId", "req-fixed-abc123",
                "toolName", "sentiment_analysis",
                "toolStatus", "success"));
        TaskResult result = worker.execute(task);

        assertEquals("req-fixed-abc123", result.getOutputData().get("requestId"));
    }

    @Test
    void returnsLoggedTrue() {
        Task task = taskWith(Map.of(
                "requestId", "req-1",
                "toolName", "test_tool",
                "toolStatus", "success"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void returnsLogEntryWithCorrectFields() {
        Task task = taskWith(Map.of(
                "requestId", "req-fixed-abc123",
                "toolName", "sentiment_analysis",
                "executionTimeMs", 187,
                "toolStatus", "success"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> logEntry = (Map<String, Object>) result.getOutputData().get("logEntry");
        assertNotNull(logEntry);
        assertEquals("req-fixed-abc123", logEntry.get("requestId"));
        assertEquals("sentiment_analysis", logEntry.get("toolName"));
        assertEquals("success", logEntry.get("toolStatus"));
        assertEquals(187, logEntry.get("executionTimeMs"));
        assertEquals("response", logEntry.get("direction"));
    }

    @Test
    void handlesNullRequestId() {
        Map<String, Object> input = new HashMap<>();
        input.put("requestId", null);
        input.put("toolName", "test_tool");
        input.put("toolStatus", "success");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("req-unknown", result.getOutputData().get("requestId"));
    }

    @Test
    void handlesNonNumericExecutionTime() {
        Task task = taskWith(Map.of(
                "requestId", "req-1",
                "toolName", "test_tool",
                "executionTimeMs", "not-a-number",
                "toolStatus", "success"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> logEntry = (Map<String, Object>) result.getOutputData().get("logEntry");
        assertEquals(0, logEntry.get("executionTimeMs"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("logEntry"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
