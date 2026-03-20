package dataencryption.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StoreKeyRefWorkerTest {

    private final StoreKeyRefWorker worker = new StoreKeyRefWorker();

    @Test
    void taskDefName() {
        assertEquals("dn_store_key_ref", worker.getTaskDefName());
    }

    @Test
    void storesKeyReference() {
        Task task = taskWith(Map.of("keyId", "KEY-ABC", "algorithm", "AES-256", "encryptedRecordCount", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("stored"));
        assertEquals("key-vault-prod", result.getOutputData().get("vault"));
        assertEquals("KEY-ABC", result.getOutputData().get("keyId"));
    }

    @Test
    void handlesDefaultValues() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("stored"));
    }

    @Test
    void preservesKeyId() {
        Task task = taskWith(Map.of("keyId", "KEY-XYZ123"));
        TaskResult result = worker.execute(task);

        assertEquals("KEY-XYZ123", result.getOutputData().get("keyId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
