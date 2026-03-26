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
        String userId = (String) task.getInputData().get("userId");

        if (token == null || token.isBlank()) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Input 'resetToken' is required and must not be blank");
            return fail;
        }

        boolean tokenValid = token.length() >= 8; // minimum token length
        boolean expired = false;
        if (expiresAtStr != null) {
            try {
                Instant expiresAt = Instant.parse(expiresAtStr);
                expired = Instant.now().isAfter(expiresAt);
            } catch (Exception e) {
                TaskResult fail = new TaskResult(task);
                fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
                fail.setReasonForIncompletion("Invalid expiresAt format: " + expiresAtStr);
                return fail;
            }
        }

        boolean verified = tokenValid && !expired;

        System.out.println("  [pwd_verify] Token " + (verified ? "valid" : "invalid") + " (expired: " + expired + ")");

        TaskResult result = new TaskResult(task);
        if (!verified) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            if (expired) {
                result.setReasonForIncompletion("Reset token has expired");
            } else {
                result.setReasonForIncompletion("Reset token is invalid (too short)");
            }
        } else {
            result.setStatus(TaskResult.Status.COMPLETED);
        }
        result.getOutputData().put("verified", verified);
        result.getOutputData().put("expired", expired);
        result.getOutputData().put("userId", userId != null ? userId : "unknown");
        result.getOutputData().put("verifiedAt", Instant.now().toString());
        return result;
    }
}
