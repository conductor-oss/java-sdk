package ragaccesscontrol.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Worker that authenticates a user by validating their auth token.
 * Returns authenticated status, roles, and clearance level.
 */
public class AuthenticateUserWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ac_authenticate_user";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        String authToken = (String) task.getInputData().get("authToken");

        boolean authenticated = "valid-jwt-token-12345".equals(authToken);

        TaskResult result = new TaskResult(task);

        if (!authenticated) {
            System.out.println("  [authenticate] User " + userId + " failed authentication");
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("authenticated", false);
            result.getOutputData().put("error", "Invalid authentication token");
            return result;
        }

        List<String> roles = List.of("engineer", "team-lead");
        String clearanceLevel = "confidential";

        System.out.println("  [authenticate] User " + userId + " authenticated with roles " + roles
                + " and clearance " + clearanceLevel);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("authenticated", true);
        result.getOutputData().put("userId", userId);
        result.getOutputData().put("roles", roles);
        result.getOutputData().put("clearanceLevel", clearanceLevel);
        return result;
    }
}
