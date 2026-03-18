package travelapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Manager approval for travel over threshold.
 */
public class ManagerApproveWorker implements Worker {
    @Override public String getTaskDefName() { return "tva_manager_approve"; }

    @Override public TaskResult execute(Task task) {
        Object costObj = task.getInputData().get("estimatedCost");
        double cost = costObj instanceof Number ? ((Number) costObj).doubleValue() : 0;

        // Approve if under 10x the threshold (reasonable limit)
        boolean approved = cost < 50000;
        String approver = cost > 20000 ? "VP-Travel" : "Manager";

        System.out.println("  [manager_approve] $" + (int) cost + " " + (approved ? "approved" : "rejected") + " by " + approver);

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("managerApproved", approved);
        r.getOutputData().put("approver", approver);
        r.getOutputData().put("approvedAt", Instant.now().toString());
        return r;
    }
}
