package dataencryption.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateKeyWorkerTest {

    private final GenerateKeyWorker worker = new GenerateKeyWorker();

    @Test
    void taskDefName() {
        assertEquals("dn_generate_key", worker.getTaskDefName());
    }

    @Test
    void generatesKeyWithAlgorithm() {
        Task task = taskWith(Map.of("algorithm", "AES-256-GCM"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String keyId = (String) result.getOutputData().get("keyId");
        assertTrue(keyId.startsWith("KEY-"));
        assertEquals("AES-256-GCM", result.getOutputData().get("algorithm"));
    }

    @Test
    void defaultsToAES256() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals("AES-256", result.getOutputData().get("algorithm"));
    }

    @Test
    void includesCreatedAt() {
        Task task = taskWith(Map.of("algorithm", "RSA"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("createdAt"));
    }

    @Test
    void generatesUniqueKeys() {
        Task task1 = taskWith(Map.of("algorithm", "AES-256"));
        Task task2 = taskWith(Map.of("algorithm", "AES-256"));
        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertNotEquals(result1.getOutputData().get("keyId"), result2.getOutputData().get("keyId"));
    }

    @Test
    void keyIdHasCorrectFormat() {
        Task task = taskWith(Map.of("algorithm", "AES-256"));
        TaskResult result = worker.execute(task);

        String keyId = (String) result.getOutputData().get("keyId");
        assertTrue(keyId.matches("KEY-[A-Z0-9]+"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
