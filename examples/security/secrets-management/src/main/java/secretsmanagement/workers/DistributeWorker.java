package secretsmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Distributes secret to target systems. Computes integrity hash for verification.
 */
public class DistributeWorker implements Worker {
    @Override public String getTaskDefName() { return "sec_distribute"; }

    @Override public TaskResult execute(Task task) {
        String secretId = (String) task.getInputData().get("secretId");
        String secretValue = (String) task.getInputData().get("secretValue");
        if (secretId == null) secretId = "UNKNOWN";
        if (secretValue == null) secretValue = "";

        // Compute integrity hash of the secret
        String integrityHash;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(secretValue.getBytes());
            integrityHash = HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (Exception e) {
            integrityHash = Integer.toHexString(secretValue.hashCode());
        }

        System.out.println("  [distribute] " + secretId + " distributed (hash: " + integrityHash + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("distributed", true);
        result.getOutputData().put("integrityHash", integrityHash);
        result.getOutputData().put("distributedAt", Instant.now().toString());
        return result;
    }
}
