package multifactorauth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PrimaryAuthWorker implements Worker {
    @Override public String getTaskDefName() { return "mfa_primary_auth"; }

    @Override public TaskResult execute(Task task) {
        String username = (String) task.getInputData().get("username");
        System.out.println("  [primary] Password authentication passed for " + username);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("userId", "USR-MFA01");
        result.getOutputData().put("primaryPassed", true);
        return result;
    }
}
