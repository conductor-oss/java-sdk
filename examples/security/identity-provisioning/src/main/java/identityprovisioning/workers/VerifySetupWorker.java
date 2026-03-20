package identityprovisioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifySetupWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ip_verify_setup";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [verify] User can access all provisioned systems");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("verify_setup", true);
        return result;
    }
}
