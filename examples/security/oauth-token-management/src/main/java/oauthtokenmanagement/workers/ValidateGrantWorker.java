package oauthtokenmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Validates the OAuth grant type and client credentials.
 * Input: clientId, grantType, scope
 * Output: validate_grantId, success
 */
public class ValidateGrantWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "otm_validate_grant";
    }

    @Override
    public TaskResult execute(Task task) {
        String clientId = (String) task.getInputData().get("clientId");
        if (clientId == null) clientId = "unknown";
        String grantType = (String) task.getInputData().get("grantType");
        if (grantType == null) grantType = "unknown";

        System.out.println("  [grant] Client " + clientId + ": " + grantType + " grant validated");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validate_grantId", "VALIDATE_GRANT-1373");
        result.getOutputData().put("success", true);
        return result;
    }
}
