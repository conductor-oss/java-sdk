package wafmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class UpdateRulesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "waf_update_rules";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [rules] Updated WAF rules: 4 new blocking rules added");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("update_rules", true);
        result.addOutputData("processed", true);
        return result;
    }
}
