package zerotrustverification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyIdentityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "zt_verify_identity";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [identity] engineer-01: MFA verified, trust score 95");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("verify_identityId", "VERIFY_IDENTITY-1366");
        result.addOutputData("success", true);
        return result;
    }
}
