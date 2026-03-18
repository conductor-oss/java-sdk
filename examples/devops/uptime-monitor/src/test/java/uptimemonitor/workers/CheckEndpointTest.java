package uptimemonitor.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CheckEndpointTest {

    private final CheckEndpoint worker = new CheckEndpoint();

    @Test
    void taskDefName() {
        assertEquals("uptime_check_endpoint", worker.getTaskDefName());
    }

    @Test
    void invalidDomainReturnsDown() {
        // .invalid TLD always fails DNS — deterministic, no internet dependency
        Task task = taskWith("https://test.invalid", "BadHost", 200, 3000);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("down", result.getOutputData().get("status"));
        assertEquals("BadHost", result.getOutputData().get("name"));
        assertEquals("https://test.invalid", result.getOutputData().get("url"));

        @SuppressWarnings("unchecked")
        var failedChecks = (List<String>) result.getOutputData().get("failedChecks");
        assertTrue(failedChecks.contains("dns"));
    }

    @Test
    void outputContainsExpectedFields() {
        Task task = taskWith("https://test.invalid", "TestEndpoint", 200, 2000);

        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("timestamp"));
        assertNotNull(result.getOutputData().get("passedChecks"));
        assertNotNull(result.getOutputData().get("failedChecks"));
        assertNotNull(result.getOutputData().get("status"));
        assertNotNull(result.getOutputData().get("responseTimeMs"));
    }

    @Test
    void defaultsAppliedWhenInputsMissing() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("url", "https://test.invalid");
        input.put("name", "Defaults");
        // No expectedStatus or timeout — should use defaults (200, 5000)
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // Should still produce a result even with missing optional inputs
        assertNotNull(result.getOutputData().get("status"));
    }

    @Test
    void responseTimeIsZeroWhenHttpFails() {
        Task task = taskWith("https://test.invalid", "FailedEndpoint", 200, 2000);

        TaskResult result = worker.execute(task);

        Object responseTime = result.getOutputData().get("responseTimeMs");
        assertNotNull(responseTime);
        assertEquals(0L, ((Number) responseTime).longValue());
    }

    private Task taskWith(String url, String name, int expectedStatus, int timeout) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("url", url);
        input.put("name", name);
        input.put("expectedStatus", expectedStatus);
        input.put("timeout", timeout);
        task.setInputData(input);
        return task;
    }
}
