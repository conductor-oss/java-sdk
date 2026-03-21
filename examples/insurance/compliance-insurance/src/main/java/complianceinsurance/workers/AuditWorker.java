package complianceinsurance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AuditWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cpi_audit";
    }

    @Override
    public TaskResult execute(Task task) {

        String companyId = (String) task.getInputData().get("companyId");
        System.out.printf("  [audit] Internal audit for %s%n", companyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("findings", java.util.Map.of("areasReviewed", 8, "issuesFound", 2, "critical", 0));
        return result;
    }
}
