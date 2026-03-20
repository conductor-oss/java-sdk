package identityprovisioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CreateIdentityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ip_create_identity";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [identity] Created identity for jane.doe in engineering");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("create_identityId", "CREATE_IDENTITY-1358");
        result.addOutputData("success", true);
        return result;
    }
}
