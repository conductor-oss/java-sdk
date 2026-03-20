package dataencryption.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encrypts sensitive fields in records (deterministic.with Base64 encoding).
 * Input: records (list), fieldsToEncrypt (list), keyId (string)
 * Output: encryptedRecords (list), encryptedCount (int), fieldsEncrypted (int), totalFieldValues (int)
 */
public class EncryptFieldsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dn_encrypt_fields";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }
        List<String> fields = (List<String>) task.getInputData().get("fieldsToEncrypt");
        if (fields == null) {
            fields = List.of();
        }
        String keyId = (String) task.getInputData().getOrDefault("keyId", "unknown");

        int totalEncrypted = 0;
        List<Map<String, Object>> encrypted = new ArrayList<>();
        for (Map<String, Object> record : records) {
            Map<String, Object> copy = new HashMap<>(record);
            for (String field : fields) {
                if (copy.containsKey(field) && copy.get(field) != null) {
                    String encoded = Base64.getEncoder().encodeToString(
                            String.valueOf(copy.get(field)).getBytes());
                    copy.put(field, "ENC[" + encoded + "]");
                    totalEncrypted++;
                }
            }
            copy.put("_encryptionKeyId", keyId);
            encrypted.add(copy);
        }

        System.out.println("  [encrypt] Encrypted " + totalEncrypted + " field values across " + records.size() + " records");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("encryptedRecords", encrypted);
        result.getOutputData().put("encryptedCount", records.size());
        result.getOutputData().put("fieldsEncrypted", fields.size());
        result.getOutputData().put("totalFieldValues", totalEncrypted);
        return result;
    }
}
