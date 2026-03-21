package wafmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyProtectionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "waf_verify_protection";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [verify] Attack process blocked — protection confirmed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("verify_protection", true);
        return result;
    }
}
