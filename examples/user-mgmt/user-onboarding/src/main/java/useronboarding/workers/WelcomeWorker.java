package useronboarding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Sends welcome email. Real template generation.
 */
public class WelcomeWorker implements Worker {
    @Override public String getTaskDefName() { return "uo_welcome"; }

    @Override public TaskResult execute(Task task) {
        String username = (String) task.getInputData().get("username");
        String userId = (String) task.getInputData().get("userId");
        if (username == null) username = "User";
        if (userId == null) userId = "UNKNOWN";

        String emailSubject = "Welcome to the platform, " + username + "!";
        String emailBody = "Hi " + username + ",\n\n"
                + "Welcome! Your account (" + userId + ") is all set up.\n\n"
                + "Here's what you can do next:\n"
                + "1. Complete your profile\n"
                + "2. Explore our features\n"
                + "3. Join the community\n\n"
                + "Happy exploring!";

        System.out.println("  [welcome] Welcome email sent to " + username);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("welcomeSent", true);
        result.getOutputData().put("emailSubject", emailSubject);
        result.getOutputData().put("emailBody", emailBody);
        result.getOutputData().put("sentAt", Instant.now().toString());
        return result;
    }
}
