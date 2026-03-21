package authorizationrbac.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Resolves user roles from the identity store.
 * Input: userId, resource, action
 * Output: resolve_rolesId, success
 */
public class ResolveRolesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rbac_resolve_roles";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        if (userId == null) userId = "unknown";

        System.out.println("  [roles] User " + userId + ": roles = [admin, editor]");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("resolve_rolesId", "RESOLVE_ROLES-1372");
        result.getOutputData().put("success", true);
        return result;
    }
}
