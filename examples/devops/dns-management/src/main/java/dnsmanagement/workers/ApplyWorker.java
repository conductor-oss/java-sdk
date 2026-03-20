package dnsmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ApplyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dns_apply";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [apply] DNS records updated in Route53");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("apply", true);
        result.addOutputData("processed", true);
        return result;
    }
}
