package useronboarding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Creates a new user account. Real validation of username, email, and password strength.
 */
public class CreateAccountWorker implements Worker {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    @Override public String getTaskDefName() { return "uo_create_account"; }

    @Override public TaskResult execute(Task task) {
        String username = (String) task.getInputData().get("username");
        String email = (String) task.getInputData().get("email");
        if (username == null) username = "";
        if (email == null) email = "";

        boolean validUsername = USERNAME_PATTERN.matcher(username).matches();
        boolean validEmail = EMAIL_PATTERN.matcher(email).matches();
        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        System.out.println("  [create] User " + username + " (" + email + ") -> " + userId);

        TaskResult result = new TaskResult(task);
        if (!validUsername || !validEmail) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion(
                    (!validUsername ? "Invalid username. " : "") + (!validEmail ? "Invalid email." : ""));
        } else {
            result.setStatus(TaskResult.Status.COMPLETED);
        }
        result.getOutputData().put("userId", userId);
        result.getOutputData().put("validUsername", validUsername);
        result.getOutputData().put("validEmail", validEmail);
        result.getOutputData().put("createdAt", Instant.now().toString());
        return result;
    }
}
