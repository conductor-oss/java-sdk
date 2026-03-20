package privilegedaccess.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Grants temporary privileged access to the requested resource.
 * Input: grant_accessData (from approve step)
 * Output: grant_access, processed
 */
public class PamGrantAccessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pam_grant_access";
    }

    @Override
    public TaskResult execute(Task task) {
        String resource = (String) task.getInputData().get("resource");
        if (resource == null) resource = "unknown-resource";

        String duration = (String) task.getInputData().get("duration");
        if (duration == null) duration = "1h";

        System.out.println("  [grant] Temporary " + resource + " access granted for " + duration);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("grant_access", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
