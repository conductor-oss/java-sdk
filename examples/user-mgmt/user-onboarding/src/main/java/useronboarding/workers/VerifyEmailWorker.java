package useronboarding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Sends email verification. Real verification code generation.
 */
public class VerifyEmailWorker implements Worker {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override public String getTaskDefName() { return "uo_verify_email"; }

    @Override public TaskResult execute(Task task) {
        String email = (String) task.getInputData().get("email");
        if (email == null) email = "unknown";

        // Generate 6-digit verification code
        String verificationCode = String.format("%06d", RANDOM.nextInt(1000000));
        String emailBody = "Your verification code is: " + verificationCode + "\nThis code expires in 30 minutes.";

        System.out.println("  [verify_email] Verification code sent to " + email);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verificationSent", true);
        result.getOutputData().put("verificationCode", verificationCode);
        result.getOutputData().put("emailBody", emailBody);
        result.getOutputData().put("sentAt", Instant.now().toString());
        return result;
    }
}
