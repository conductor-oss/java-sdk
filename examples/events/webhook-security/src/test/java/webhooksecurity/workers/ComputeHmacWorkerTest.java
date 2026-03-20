package webhooksecurity.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComputeHmacWorkerTest {

    private final ComputeHmacWorker worker = new ComputeHmacWorker();

    @Test
    void taskDefName() {
        assertEquals("ws_compute_hmac", worker.getTaskDefName());
    }

    @Test
    void computesHmacWithValidInputs() {
        Task task = taskWith(Map.of("payload", "{\"event\":\"push\"}", "secret", "my-secret"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("hmac_sha256_fixedvalue", result.getOutputData().get("computedSignature"));
        assertEquals("sha256", result.getOutputData().get("algorithm"));
    }

    @Test
    void computesHmacWithEmptyPayload() {
        Task task = taskWith(Map.of("payload", "", "secret", "my-secret"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("hmac_sha256_fixedvalue", result.getOutputData().get("computedSignature"));
        assertEquals("sha256", result.getOutputData().get("algorithm"));
    }

    @Test
    void computesHmacWithEmptySecret() {
        Task task = taskWith(Map.of("payload", "{\"event\":\"push\"}", "secret", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("hmac_sha256_fixedvalue", result.getOutputData().get("computedSignature"));
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("payload", null);
        input.put("secret", "my-secret");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("hmac_sha256_fixedvalue", result.getOutputData().get("computedSignature"));
        assertEquals("sha256", result.getOutputData().get("algorithm"));
    }

    @Test
    void handlesNullSecret() {
        Map<String, Object> input = new HashMap<>();
        input.put("payload", "{\"event\":\"push\"}");
        input.put("secret", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("hmac_sha256_fixedvalue", result.getOutputData().get("computedSignature"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("hmac_sha256_fixedvalue", result.getOutputData().get("computedSignature"));
        assertEquals("sha256", result.getOutputData().get("algorithm"));
    }

    @Test
    void algorithmIsAlwaysSha256() {
        Task task = taskWith(Map.of("payload", "any-payload", "secret", "any-secret"));
        TaskResult result = worker.execute(task);

        assertEquals("sha256", result.getOutputData().get("algorithm"));
    }

    @Test
    void computesHmacWithLargePayload() {
        String largePayload = "x".repeat(10000);
        Task task = taskWith(Map.of("payload", largePayload, "secret", "key"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("hmac_sha256_fixedvalue", result.getOutputData().get("computedSignature"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
