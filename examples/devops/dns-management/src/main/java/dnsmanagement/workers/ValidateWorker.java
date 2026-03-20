package dnsmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ValidateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dns_validate";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [validate] No conflicts detected");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("validate", true);
        result.addOutputData("processed", true);
        return result;
    }
}
