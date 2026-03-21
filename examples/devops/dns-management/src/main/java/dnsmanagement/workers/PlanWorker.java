package dnsmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PlanWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dns_plan";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [plan] DNS change planned");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("planId", "PLAN-1345");
        result.addOutputData("success", true);
        return result;
    }
}
