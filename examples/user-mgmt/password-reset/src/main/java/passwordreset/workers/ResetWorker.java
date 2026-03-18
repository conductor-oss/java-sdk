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
        // Check that the token was verified
        Object verifiedObj = task.getInputData().get("verified");
        boolean verified = Boolean.TRUE.equals(verifiedObj)
                || "true".equalsIgnoreCase(String.valueOf(verifiedObj));
        if (!verified) {
            System.out.println("  [pwd_reset] Rejected — token not verified");
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED);
            fail.setReasonForIncompletion("Reset token was not verified");
            fail.getOutputData().put("resetSuccess", false);
            return fail;
        }

        String newPassword = (String) task.getInputData().get("newPassword");
        if (newPassword == null) newPassword = "";

        // Real password strength validation
        boolean hasMinLength = newPassword.length() >= 8;
        boolean hasUpperCase = newPassword.chars().anyMatch(Character::isUpperCase);
        boolean hasLowerCase = newPassword.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = newPassword.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = newPassword.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0);
        int strength = (hasMinLength ? 1 : 0) + (hasUpperCase ? 1 : 0) + (hasLowerCase ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
        boolean strongEnough = strength >= 3 && hasMinLength;

        // Real PBKDF2 password hashing
        String passwordHash = "";
        String salt = "";
        if (strongEnough && !newPassword.isEmpty()) {
            try {
                byte[] saltBytes = new byte[16];
                SECURE_RANDOM.nextBytes(saltBytes);
                salt = Base64.getEncoder().encodeToString(saltBytes);
                PBEKeySpec spec = new PBEKeySpec(newPassword.toCharArray(), saltBytes, 65536, 256);
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] hash = factory.generateSecret(spec).getEncoded();
                passwordHash = Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                passwordHash = "HASH_ERROR";
            }
        }

        System.out.println("  [pwd_reset] Password strength: " + strength + "/5, strong enough: " + strongEnough);

        TaskResult result = new TaskResult(task);
        result.setStatus(strongEnough ? TaskResult.Status.COMPLETED : TaskResult.Status.FAILED);
        if (!strongEnough) result.setReasonForIncompletion("Password too weak (strength: " + strength + "/5)");
        result.getOutputData().put("resetSuccess", strongEnough);
        result.getOutputData().put("passwordHash", passwordHash);
        result.getOutputData().put("salt", salt);
        result.getOutputData().put("strength", strength);
        result.getOutputData().put("resetAt", Instant.now().toString());
        return result;
    }
}
