package identityprovisioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ProvisionAccessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ip_provision_access";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [access] Provisioned: GitHub, AWS, Slack, Jira");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("provision_access", true);
        result.addOutputData("processed", true);
        return result;
    }
}
