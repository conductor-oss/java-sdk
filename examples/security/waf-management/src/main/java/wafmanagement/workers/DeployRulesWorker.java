package wafmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeployRulesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "waf_deploy_rules";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [deploy] Rules deployed to CloudFront WAF");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("deploy_rules", true);
        result.addOutputData("processed", true);
        return result;
    }
}
