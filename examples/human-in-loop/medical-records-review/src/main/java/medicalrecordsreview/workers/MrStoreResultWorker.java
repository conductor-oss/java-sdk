package medicalrecordsreview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for mr_store_result task -- stores the physician review result.
 *
 * Returns stored=true and a deterministic auditTrailId="AUDIT-001".
 */
public class MrStoreResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mr_store_result";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [mr_store_result] Storing physician review result...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("stored", true);
        result.getOutputData().put("auditTrailId", "AUDIT-001");

        System.out.println("  [mr_store_result] Result stored. Audit trail ID: AUDIT-001");
        return result;
    }
}
