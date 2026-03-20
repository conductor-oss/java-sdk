package webhooksecurity.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifySignatureWorkerTest {

    private final VerifySignatureWorker worker = new VerifySignatureWorker();

    @Test
    void taskDefName() {
        assertEquals("ws_verify_signature", worker.getTaskDefName());
    }

    @Test
    void validWhenSignaturesMatch() {
        Task task = taskWith(Map.of("expected", "hmac_sha256_fixedvalue", "provided", "hmac_sha256_fixedvalue"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("valid", result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("match"));
    }

    @Test
    void invalidWhenSignaturesMismatch() {
        Task task = taskWith(Map.of("expected", "hmac_sha256_fixedvalue", "provided", "wrong_signature"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("invalid", result.getOutputData().get("result"));
        assertEquals(false, result.getOutputData().get("match"));
    }

    @Test
    void invalidWhenProvidedIsEmpty() {
        Task task = taskWith(Map.of("expected", "hmac_sha256_fixedvalue", "provided", ""));
        TaskResult result = worker.execute(task);

        assertEquals("invalid", result.getOutputData().get("result"));
        assertEquals(false, result.getOutputData().get("match"));
    }

    @Test
    void validWhenBothEmpty() {
        Task task = taskWith(Map.of("expected", "", "provided", ""));
        TaskResult result = worker.execute(task);

        assertEquals("valid", result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("match"));
    }

    @Test
    void handlesNullExpected() {
        Map<String, Object> input = new HashMap<>();
        input.put("expected", null);
        input.put("provided", "some_sig");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("invalid", result.getOutputData().get("result"));
        assertEquals(false, result.getOutputData().get("match"));
    }

    @Test
    void handlesNullProvided() {
        Map<String, Object> input = new HashMap<>();
        input.put("expected", "hmac_sha256_fixedvalue");
        input.put("provided", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("invalid", result.getOutputData().get("result"));
        assertEquals(false, result.getOutputData().get("match"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("valid", result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("match"));
    }

    @Test
    void caseSensitiveComparison() {
        Task task = taskWith(Map.of("expected", "HMAC_SHA256_FIXEDVALUE", "provided", "hmac_sha256_fixedvalue"));
        TaskResult result = worker.execute(task);

        assertEquals("invalid", result.getOutputData().get("result"));
        assertEquals(false, result.getOutputData().get("match"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
