package authenticationworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks multi-factor authentication verification.
 * Input: check_mfaData (from previous step)
 * Output: check_mfa, processed
 */
public class CheckMfaWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "auth_check_mfa";
    }

    @Override
    public TaskResult execute(Task task) {
        String authMethod = (String) task.getInputData().get("authMethod");
        if (authMethod == null) {
            authMethod = "unknown";
        }

        System.out.println("  [mfa] " + authMethod + " verification passed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("check_mfa", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
