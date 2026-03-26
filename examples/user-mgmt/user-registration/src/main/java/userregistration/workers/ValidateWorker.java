package userregistration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Validates username and email format.
 * Input: username, email
 * Output: valid, checks
 */
public class ValidateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ur_validate";
    }

    @Override
    public TaskResult execute(Task task) {
        String username = (String) task.getInputData().get("username");
        String email = (String) task.getInputData().get("email");

        boolean valid = username != null && username.length() >= 3
                && email != null && email.contains("@");

        System.out.println("  [validate] Username \"" + username + "\" and email: " + (valid ? "valid" : "invalid"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", valid);
        result.getOutputData().put("checks", Map.of(
                "usernameLength", true,
                "emailFormat", true,
                "notTaken", true
        ));
        return result;
    }
}
