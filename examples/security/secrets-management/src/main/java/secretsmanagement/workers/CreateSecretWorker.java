package secretsmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Creates cryptographically secure secrets using SecureRandom.
 */
public class CreateSecretWorker implements Worker {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override public String getTaskDefName() { return "sec_create_secret"; }

    @Override public TaskResult execute(Task task) {
        String secretName = (String) task.getInputData().get("secretName");
        Object lengthObj = task.getInputData().get("length");
        if (secretName == null) secretName = "default-secret";

        int length = 32;
        if (lengthObj instanceof Number) length = ((Number) lengthObj).intValue();
        length = Math.max(16, Math.min(256, length));

        // Generate cryptographically secure random bytes
        byte[] secretBytes = new byte[length];
        SECURE_RANDOM.nextBytes(secretBytes);
        String secretValue = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

        // Generate secret ID
        byte[] idBytes = new byte[12];
        SECURE_RANDOM.nextBytes(idBytes);
        String secretId = "SEC-" + Base64.getUrlEncoder().withoutPadding().encodeToString(idBytes).substring(0, 12);

        System.out.println("  [create_secret] " + secretName + " (" + length + " bytes) -> " + secretId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("secretId", secretId);
        result.getOutputData().put("secretName", secretName);
        result.getOutputData().put("secretValue", secretValue);
        result.getOutputData().put("lengthBytes", length);
        result.getOutputData().put("createdAt", Instant.now().toString());
        return result;
    }
}
