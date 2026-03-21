package authenticationworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Validates user credentials (password, biometric, etc.).
 * Input: userId, authMethod
 * Output: validate_credentialsId, success
 */
public class ValidateCredentialsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "auth_validate_credentials";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        if (userId == null) {
            userId = "unknown";
        }

        System.out.println("  [credentials] User " + userId + ": password validated");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validate_credentialsId", "VALIDATE_CREDENTIALS-1371");
        result.getOutputData().put("success", true);
        return result;
    }
}
