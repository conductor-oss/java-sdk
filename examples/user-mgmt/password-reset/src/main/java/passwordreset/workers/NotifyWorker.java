package passwordreset.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Sends password reset notification. Real email template generation.
 */
public class NotifyWorker implements Worker {
    @Override public String getTaskDefName() { return "pwd_notify"; }

    @Override public TaskResult execute(Task task) {
        String email = (String) task.getInputData().get("email");
        if (email == null || email.isBlank()) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Input 'email' is required and must not be blank");
            return fail;
        }

        Object resetObj = task.getInputData().get("resetSuccess");
        boolean wasReset = Boolean.TRUE.equals(resetObj);
        String userId = (String) task.getInputData().get("userId");

        String subject = wasReset ? "Your password has been reset" : "Password reset failed";
        String body = wasReset
                ? "Your password has been successfully reset. If you did not request this change, please contact support immediately."
                : "Your password reset request could not be completed. Please try again with a stronger password.";

        System.out.println("  [pwd_notify] " + email + " -> " + subject);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        result.getOutputData().put("emailSubject", subject);
        result.getOutputData().put("emailBody", body);
        result.getOutputData().put("sentAt", Instant.now().toString());
        result.getOutputData().put("userId", userId != null ? userId : "unknown");
        return result;
    }
}
