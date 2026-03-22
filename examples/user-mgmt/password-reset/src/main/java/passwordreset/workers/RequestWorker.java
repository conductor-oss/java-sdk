package passwordreset.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Handles password reset requests. Real email validation and secure token generation.
 */
public class RequestWorker implements Worker {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Override public String getTaskDefName() { return "pwd_request"; }

    @Override public TaskResult execute(Task task) {
        String email = (String) task.getInputData().get("email");
        if (email == null || email.isBlank()) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Input 'email' is required and must not be blank");
            return fail;
        }

        boolean validEmail = EMAIL_PATTERN.matcher(email).matches();
        if (!validEmail) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Invalid email format: " + email);
            return fail;
        }

        // Generate secure reset token
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String resetToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.HOURS);

        // Generate userId from email hash
        String userId = "USR-" + Integer.toHexString(email.hashCode()).toUpperCase();

        System.out.println("  [pwd_request] Reset request for " + email + " (userId: " + userId + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("userId", userId);
        result.getOutputData().put("validEmail", validEmail);
        result.getOutputData().put("resetToken", resetToken);
        result.getOutputData().put("expiresAt", expiresAt.toString());
        result.getOutputData().put("requestedAt", now.toString());
        return result;
    }
}
