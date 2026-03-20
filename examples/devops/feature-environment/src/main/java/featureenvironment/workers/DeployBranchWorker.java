package featureenvironment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeployBranchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fe_deploy_branch";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [deploy] Deployed branch to preview environment");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("deploy_branch", true);
        result.addOutputData("processed", true);
        return result;
    }
}
