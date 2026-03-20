package networksegmentation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ApplyPoliciesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ns_apply_policies";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [apply] Security group policies applied across VPC");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("apply_policies", true);
        result.addOutputData("processed", true);
        return result;
    }
}
