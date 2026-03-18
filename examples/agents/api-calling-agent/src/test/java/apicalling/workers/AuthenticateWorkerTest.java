package apicalling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticateWorkerTest {

    private final AuthenticateWorker worker = new AuthenticateWorker();

    @Test
    void taskDefName() {
        assertEquals("ap_authenticate", worker.getTaskDefName());
    }

    @Test
    void generatesRealJwt() {
        Task task = taskWith(Map.of("apiName", "github", "authType", "bearer_token"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String token = (String) result.getOutputData().get("token");
        assertNotNull(token);
        // Real JWT has 3 base64url segments separated by dots
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have header.payload.signature");
    }

    @Test
    void jwtContainsApiNameInPayload() {
        Task task = taskWith(Map.of("apiName", "stripe", "authType", "api_key"));
        TaskResult result = worker.execute(task);

        String token = (String) result.getOutputData().get("token");
        // Decode payload (second segment)
        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        assertTrue(payload.contains("stripe"), "Payload should contain the apiName");
    }

    @Test
    void returnsExpiresIn() {
        Task task = taskWith(Map.of("apiName", "github", "authType", "bearer_token"));
        TaskResult result = worker.execute(task);

        assertEquals(3600, result.getOutputData().get("expiresIn"));
    }

    @Test
    void returnsTokenType() {
        Task task = taskWith(Map.of("apiName", "github", "authType", "bearer_token"));
        TaskResult result = worker.execute(task);

        assertEquals("Bearer", result.getOutputData().get("tokenType"));
    }

    @Test
    void handlesNullApiName() {
        Map<String, Object> input = new HashMap<>();
        input.put("apiName", null);
        input.put("authType", "bearer_token");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("token"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("token"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
