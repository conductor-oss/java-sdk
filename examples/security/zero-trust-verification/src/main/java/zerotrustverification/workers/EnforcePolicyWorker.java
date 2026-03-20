package zerotrustverification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EnforcePolicyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "zt_enforce_policy";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [enforce] Access GRANTED — composite trust score: 91");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("enforce_policy", true);
        return result;
    }
}
