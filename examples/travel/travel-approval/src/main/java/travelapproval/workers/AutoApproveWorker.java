package travelapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Auto-approves travel requests under threshold. Real threshold logic.
 */
public class AutoApproveWorker implements Worker {
    @Override public String getTaskDefName() { return "tva_auto_approve"; }

    @Override public TaskResult execute(Task task) {
        Object costObj = task.getInputData().get("estimatedCost");
        Object thresholdObj = task.getInputData().get("threshold");
        double cost = costObj instanceof Number ? ((Number) costObj).doubleValue() : 0;
        double threshold = thresholdObj instanceof Number ? ((Number) thresholdObj).doubleValue() : 5000;

        boolean autoApproved = cost <= threshold;
        String needsManagerApproval = autoApproved ? "false" : "true";

        System.out.println("  [auto_approve] $" + (int) cost + " vs threshold $" + (int) threshold
                + " -> " + (autoApproved ? "auto-approved" : "needs manager approval"));

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("autoApproved", autoApproved);
        r.getOutputData().put("needsManagerApproval", needsManagerApproval);
        return r;
    }
}
