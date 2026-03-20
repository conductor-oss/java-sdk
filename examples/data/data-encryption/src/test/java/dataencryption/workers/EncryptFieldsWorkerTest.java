package dataencryption.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EncryptFieldsWorkerTest {

    private final EncryptFieldsWorker worker = new EncryptFieldsWorker();

    @Test
    void taskDefName() {
        assertEquals("dn_encrypt_fields", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void encryptsSpecifiedFields() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "ssn", "123-45-6789"));
        Task task = taskWith(Map.of("records", records, "fieldsToEncrypt", List.of("ssn"), "keyId", "KEY-TEST"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> encrypted = (List<Map<String, Object>>) result.getOutputData().get("encryptedRecords");
        String ssnValue = (String) encrypted.get(0).get("ssn");
        assertTrue(ssnValue.startsWith("ENC["));
        assertTrue(ssnValue.endsWith("]"));
        assertEquals("Alice", encrypted.get(0).get("name"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void addsKeyIdToRecords() {
        List<Map<String, Object>> records = List.of(Map.of("ssn", "111"));
        Task task = taskWith(Map.of("records", records, "fieldsToEncrypt", List.of("ssn"), "keyId", "KEY-ABC"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> encrypted = (List<Map<String, Object>>) result.getOutputData().get("encryptedRecords");
        assertEquals("KEY-ABC", encrypted.get(0).get("_encryptionKeyId"));
    }

    @Test
    void countsEncryptedFieldValues() {
        List<Map<String, Object>> records = List.of(
                Map.of("ssn", "111", "cc", "222"),
                Map.of("ssn", "333", "cc", "444"));
        Task task = taskWith(Map.of("records", records, "fieldsToEncrypt", List.of("ssn", "cc"), "keyId", "KEY-X"));
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("totalFieldValues"));
        assertEquals(2, result.getOutputData().get("encryptedCount"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of(), "fieldsToEncrypt", List.of("ssn"), "keyId", "KEY-X"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("encryptedCount"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("fieldsToEncrypt", List.of("ssn"));
        input.put("keyId", "KEY-X");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
