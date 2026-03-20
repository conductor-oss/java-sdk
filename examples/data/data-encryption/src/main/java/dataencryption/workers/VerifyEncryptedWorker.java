package dataencryption.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Verifies that all records are properly tagged with the encryption key ID.
 * Input: encryptedRecords (list), fieldsEncrypted (int), keyId (string)
 * Output: verified (boolean), checkedRecords (int)
 */
public class VerifyEncryptedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dn_verify_encrypted";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("encryptedRecords");
        if (records == null) {
            records = List.of();
        }
        String keyId = (String) task.getInputData().getOrDefault("keyId", "unknown");

        boolean allEncrypted = records.stream()
                .allMatch(r -> keyId.equals(r.get("_encryptionKeyId")));

        System.out.println("  [verify] Verification: all records tagged with key " + keyId + ": " + allEncrypted);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", allEncrypted);
        result.getOutputData().put("checkedRecords", records.size());
        return result;
    }
}
