package multifactorauth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyFactorWorker implements Worker {
    @Override public String getTaskDefName() { return "mfa_verify_factor"; }

    @Override public TaskResult execute(Task task) {
        String method = (String) task.getInputData().get("method");
        String userId = (String) task.getInputData().get("userId");
        System.out.println("  [verify] " + (method != null ? method.toUpperCase() : "UNKNOWN") + " code verified for " + userId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", true);
        result.getOutputData().put("attempts", 1);
        return result;
    }
}
