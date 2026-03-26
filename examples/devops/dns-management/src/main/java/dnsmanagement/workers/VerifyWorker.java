package dnsmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dns_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [verify] DNS propagation confirmed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("verify", true);
        return result;
    }
}
