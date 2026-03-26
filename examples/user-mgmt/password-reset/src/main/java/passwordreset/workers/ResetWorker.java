package passwordreset.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Resets password with PBKDF2 hashing. Real password strength validation and hashing.
 */
public class ResetWorker implements Worker {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override public String getTaskDefName() { return "pwd_reset"; }

    @Override public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        if (userId == null || userId.isBlank()) {
            userId = "unknown";
        }

        // Check that the token was verified
        Object verifiedObj = task.getInputData().get("verified");
        boolean verified = Boolean.TRUE.equals(verifiedObj)
                || "true".equalsIgnoreCase(String.valueOf(verifiedObj));
        if (!verified) {
            System.out.println("  [pwd_reset] Rejected — token not verified");
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Reset token was not verified");
            fail.getOutputData().put("resetSuccess", false);
            fail.getOutputData().put("resetAt", Instant.now().toString());
            fail.getOutputData().put("userId", userId);
            fail.getOutputData().put("strength", 0);
            return fail;
        }

        String newPassword = (String) task.getInputData().get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Input 'newPassword' is required and must not be blank");
            fail.getOutputData().put("resetSuccess", false);
            fail.getOutputData().put("resetAt", Instant.now().toString());
            fail.getOutputData().put("userId", userId);
            fail.getOutputData().put("strength", 0);
            return fail;
        }

        // Real password strength validation
        boolean hasMinLength = newPassword.length() >= 8;
        boolean hasUpperCase = newPassword.chars().anyMatch(Character::isUpperCase);
        boolean hasLowerCase = newPassword.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = newPassword.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = newPassword.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0);
        int strength = (hasMinLength ? 1 : 0) + (hasUpperCase ? 1 : 0) + (hasLowerCase ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
        boolean strongEnough = strength >= 3 && hasMinLength;

        if (!strongEnough) {
            System.out.println("  [pwd_reset] Password too weak — strength: " + strength + "/5");
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Password too weak (strength: " + strength + "/5). "
                    + "Requires minimum 8 chars and at least 3 of: uppercase, lowercase, digit, special char.");
            fail.getOutputData().put("resetSuccess", false);
            fail.getOutputData().put("strength", strength);
            fail.getOutputData().put("resetAt", Instant.now().toString());
            fail.getOutputData().put("userId", userId);
            return fail;
        }

        // Real PBKDF2 password hashing
        String passwordHash;
        String salt;
        try {
            byte[] saltBytes = new byte[16];
            SECURE_RANDOM.nextBytes(saltBytes);
            salt = Base64.getEncoder().encodeToString(saltBytes);
            PBEKeySpec spec = new PBEKeySpec(newPassword.toCharArray(), saltBytes, 65536, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            passwordHash = Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED);
            fail.setReasonForIncompletion("Password hashing failed: " + e.getMessage());
            fail.getOutputData().put("resetSuccess", false);
            fail.getOutputData().put("strength", strength);
            fail.getOutputData().put("resetAt", Instant.now().toString());
            fail.getOutputData().put("userId", userId);
            return fail;
        }

        System.out.println("  [pwd_reset] Password strength: " + strength + "/5, strong enough: " + strongEnough);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("resetSuccess", true);
        result.getOutputData().put("passwordHash", passwordHash);
        result.getOutputData().put("salt", salt);
        result.getOutputData().put("strength", strength);
        result.getOutputData().put("resetAt", Instant.now().toString());
        result.getOutputData().put("userId", userId);
        return result;
    }
}
