package securityposture.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssessInfrastructureWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sp_assess_infrastructure";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [infra] acme-corp: 85/100 — patching gaps found");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("assess_infrastructureId", "ASSESS_INFRASTRUCTURE-1400");
        result.addOutputData("success", true);
        return result;
    }
}
