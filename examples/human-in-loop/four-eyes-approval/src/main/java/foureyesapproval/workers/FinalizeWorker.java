package foureyesapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for fep_finalize -- finalizes the request after both approvals.
 *
 * Receives both approvers' decisions and finalizes -- proceeds only
 * if both approved, otherwise records which approver dissented.
 *
 * Outputs:
 *   - finalized: true
 *   - bothApproved: true if both approvers approved, false otherwise
 *   - approval1: first approver's decision
 *   - approval2: second approver's decision
 */
public class FinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fep_finalize";
    }

    @Override
    public TaskResult execute(Task task) {
        Object a1 = task.getInputData().get("approval1");
        Object a2 = task.getInputData().get("approval2");

        boolean approved1 = Boolean.TRUE.equals(a1) || "true".equals(String.valueOf(a1));
        boolean approved2 = Boolean.TRUE.equals(a2) || "true".equals(String.valueOf(a2));
        boolean bothApproved = approved1 && approved2;

        System.out.println("  [fep_finalize] Approver 1: " + approved1 + ", Approver 2: " + approved2
                + " -> " + (bothApproved ? "APPROVED" : "REJECTED"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalized", true);
        result.getOutputData().put("bothApproved", bothApproved);
        result.getOutputData().put("approval1", approved1);
        result.getOutputData().put("approval2", approved2);
        return result;
    }
}
