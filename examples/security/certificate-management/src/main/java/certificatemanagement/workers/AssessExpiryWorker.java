package certificatemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssessExpiryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cm_assess_expiry";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [expiry] 5 certificates expiring within renewal window");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("assess_expiry", true);
        result.addOutputData("processed", true);
        return result;
    }
}
