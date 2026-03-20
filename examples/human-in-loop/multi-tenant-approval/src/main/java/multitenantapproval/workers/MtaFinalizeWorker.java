package multitenantapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for mta_finalize task -- finalizes the approval process
 * and returns processed=true.
 */
public class MtaFinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mta_finalize";
    }

    @Override
    public TaskResult execute(Task task) {
        String tenantId = (String) task.getInputData().get("tenantId");
        String approvalLevel = (String) task.getInputData().get("approvalLevel");

        System.out.println("  [mta_finalize] Finalizing for tenant=" + tenantId
                + ", approvalLevel=" + approvalLevel);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);

        System.out.println("  [mta_finalize] Finalization complete.");
        return result;
    }
}
