package kycaml.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Makes a compliance decision based on the assessed risk level.
 */
public class DecideWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "kyc_decide";
    }

    @Override
    public TaskResult execute(Task task) {
        String riskLevel = (String) task.getInputData().get("riskLevel");
        if (riskLevel == null) riskLevel = "medium";

        String decision;
        if ("low".equals(riskLevel)) {
            decision = "approved";
        } else if ("medium".equals(riskLevel)) {
            decision = "approved_with_monitoring";
        } else {
            decision = "enhanced_due_diligence";
        }

        System.out.println("  [decide] Risk: " + riskLevel + ", Decision: " + decision);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("decision", decision);
        result.getOutputData().put("reviewRequired", "high".equals(riskLevel));
        result.getOutputData().put("decidedAt", Instant.now().toString());
        return result;
    }
}
