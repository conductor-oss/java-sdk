package datacompression.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyIntegrityWorkerTest {

    private final VerifyIntegrityWorker worker = new VerifyIntegrityWorker();

    @Test
    void taskDefName() {
        assertEquals("cmp_verify_integrity", worker.getTaskDefName());
    }

    @Test
    void passesWithValidChecksum() {
        Task task = taskWith(Map.of("checksum", "sha256:abc123", "compressedSize", 500));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("integrityOk"));
    }

    @Test
    void failsWithMissingChecksum() {
        Map<String, Object> input = new HashMap<>();
        input.put("checksum", null);
        input.put("compressedSize", 500);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("integrityOk"));
    }

    @Test
    void failsWithZeroSize() {
        Task task = taskWith(Map.of("checksum", "sha256:abc", "compressedSize", 0));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("integrityOk"));
    }

    @Test
    void preservesChecksum() {
        Task task = taskWith(Map.of("checksum", "sha256:xyz789", "compressedSize", 100));
        TaskResult result = worker.execute(task);

        assertEquals("sha256:xyz789", result.getOutputData().get("checksum"));
    }

    @Test
    void failsWithEmptyChecksum() {
        Task task = taskWith(Map.of("checksum", "", "compressedSize", 100));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("integrityOk"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
