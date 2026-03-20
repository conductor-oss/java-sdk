package dataencryption.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyEncryptedWorkerTest {

    private final VerifyEncryptedWorker worker = new VerifyEncryptedWorker();

    @Test
    void taskDefName() {
        assertEquals("dn_verify_encrypted", worker.getTaskDefName());
    }

    @Test
    void verifiesWhenAllTagged() {
        List<Map<String, Object>> records = List.of(
                Map.of("_encryptionKeyId", "KEY-ABC", "name", "Alice"),
                Map.of("_encryptionKeyId", "KEY-ABC", "name", "Bob"));
        Task task = taskWith(Map.of("encryptedRecords", records, "keyId", "KEY-ABC"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
        assertEquals(2, result.getOutputData().get("checkedRecords"));
    }

    @Test
    void failsWhenKeyMismatch() {
        List<Map<String, Object>> records = List.of(
                Map.of("_encryptionKeyId", "KEY-ABC"),
                Map.of("_encryptionKeyId", "KEY-WRONG"));
        Task task = taskWith(Map.of("encryptedRecords", records, "keyId", "KEY-ABC"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("encryptedRecords", List.of(), "keyId", "KEY-ABC"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("encryptedRecords", null);
        input.put("keyId", "KEY-ABC");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("checkedRecords"));
    }

    @Test
    void checksCorrectRecordCount() {
        List<Map<String, Object>> records = List.of(
                Map.of("_encryptionKeyId", "K1"),
                Map.of("_encryptionKeyId", "K1"),
                Map.of("_encryptionKeyId", "K1"));
        Task task = taskWith(Map.of("encryptedRecords", records, "keyId", "K1"));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("checkedRecords"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
