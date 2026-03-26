package authorizationrbac.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Evaluates permissions for the requested action on the resource.
 * Input: evaluate_permissionsData
 * Output: evaluate_permissions, processed
 */
public class EvaluatePermissionsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rbac_evaluate_permissions";
    }

    @Override
    public TaskResult execute(Task task) {
        String action = (String) task.getInputData().get("action");
        if (action == null) action = "unknown";
        String resource = (String) task.getInputData().get("resource");
        if (resource == null) resource = "unknown";

        System.out.println("  [permissions] " + action + " on " + resource + ": permitted");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("evaluate_permissions", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
