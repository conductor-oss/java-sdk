package passwordreset.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Verifies password reset token. Real expiration checking.
 */
public class VerifyTokenWorker implements Worker {
    @Override public String getTaskDefName() { return "pwd_verify"; }

    @Override public TaskResult execute(Task task) {
        String token = (String) task.getInputData().get("resetToken");
        String expiresAtStr = (String) task.getInputData().get("expiresAt");
        if (token == null) token = "";

        boolean tokenValid = token.length() >= 8; // minimum token length
        boolean expired = false;
        if (expiresAtStr != null) {
            try {
                Instant expiresAt = Instant.parse(expiresAtStr);
                expired = Instant.now().isAfter(expiresAt);
            } catch (Exception ignored) {}
        }

        boolean verified = tokenValid && !expired;

        System.out.println("  [pwd_verify] Token " + (verified ? "valid" : "invalid") + " (expired: " + expired + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", verified);
        result.getOutputData().put("expired", expired);
        return result;
    }
}
