package dataencryption.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Stores the encryption key reference in a vault.
 * Input: keyId (string), algorithm (string), encryptedRecordCount (int)
 * Output: stored (boolean), vault (string), keyId (string)
 */
public class StoreKeyRefWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dn_store_key_ref";
    }

    @Override
    public TaskResult execute(Task task) {
        String keyId = (String) task.getInputData().getOrDefault("keyId", "unknown");
        String algorithm = (String) task.getInputData().getOrDefault("algorithm", "unknown");
        Object countObj = task.getInputData().get("encryptedRecordCount");
        int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;

        System.out.println("  [store-key] Stored key reference: " + keyId + " (" + algorithm + ") for " + count + " records");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("stored", true);
        result.getOutputData().put("vault", "key-vault-prod");
        result.getOutputData().put("keyId", keyId);
        return result;
    }
}
