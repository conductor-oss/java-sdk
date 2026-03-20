package featureenvironment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ProvisionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fe_provision";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [provision] Created namespace for branch feature-auth-v2");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("provisionId", "PROVISION-1360");
        result.addOutputData("success", true);
        return result;
    }
}
