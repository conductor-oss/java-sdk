package gdprdatadeletion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Verifies the identity of the user requesting data deletion.
 * Input: userId, verificationToken
 * Output: verified (boolean), method
 */
public class VerifyIdentityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gr_verify_identity";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().getOrDefault("userId", "unknown");
        String token = (String) task.getInputData().getOrDefault("verificationToken", "");
        boolean verified = token != null && !token.isEmpty();

        System.out.println("  [verify] Identity verification for " + userId + ": "
                + (verified ? "VERIFIED" : "FAILED"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", verified);
        result.getOutputData().put("method", "token_verification");
        return result;
    }
}
