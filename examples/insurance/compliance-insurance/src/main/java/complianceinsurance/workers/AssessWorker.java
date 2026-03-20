package complianceinsurance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cpi_assess";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [assess] 2 minor issues found — remediation plan created");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assessment", "compliant-with-findings");
        result.getOutputData().put("remediationItems", 2);
        result.getOutputData().put("riskLevel", "low");
        return result;
    }
}
