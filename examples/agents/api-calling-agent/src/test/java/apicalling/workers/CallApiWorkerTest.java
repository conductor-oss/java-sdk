package apicalling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CallApiWorkerTest {

    private final CallApiWorker worker = new CallApiWorker();

    @Test
    void taskDefName() {
        assertEquals("ap_call_api", worker.getTaskDefName());
    }

    @Test
    void makesRealHttpRequest() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");

        Task task = taskWith(Map.of(
                "endpoint", "https://api.github.com/repos/conductor-oss/conductor",
                "method", "GET"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) result.getOutputData().get("response");
        assertNotNull(response);
        assertNotNull(response.get("bodyPreview"), "Should have bodyPreview from real HTTP response");
        assertTrue(((Number) response.get("bodyLength")).intValue() > 0, "Body should be non-empty");
        assertNotNull(response.get("contentType"));
    }

    @Test
    void returnsStatusCodeFromRealEndpoint() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");

        Task task = taskWith(Map.of("endpoint", "https://api.github.com/repos/conductor-oss/conductor", "method", "GET"));
        TaskResult result = worker.execute(task);

        int statusCode = ((Number) result.getOutputData().get("statusCode")).intValue();
        assertTrue(statusCode > 0, "Should return a real HTTP status code");
    }

    @Test
    void measuresRealResponseTime() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");

        Task task = taskWith(Map.of("endpoint", "https://api.github.com/repos/conductor-oss/conductor"));
        TaskResult result = worker.execute(task);

        long responseTime = ((Number) result.getOutputData().get("responseTimeMs")).longValue();
        assertTrue(responseTime >= 0, "Response time must be non-negative");
        assertTrue(responseTime < 30000, "Response time should be under 30 seconds");
    }

    @Test
    void handlesNullEndpointGracefully() {
        Map<String, Object> input = new HashMap<>();
        input.put("endpoint", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("response"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("response"));
    }

    private static boolean isNetworkAvailable() {
        try { InetAddress.getByName("api.github.com"); return true; } catch (Exception e) { return false; }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
