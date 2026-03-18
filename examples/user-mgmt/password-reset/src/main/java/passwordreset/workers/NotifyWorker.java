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
        Object resetObj = task.getInputData().get("resetSuccess");
        if (email == null) email = "unknown";
        boolean wasReset = Boolean.TRUE.equals(resetObj);

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
        return result;
    }
}
