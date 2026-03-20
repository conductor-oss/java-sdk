package multistepcompensation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for msc_setup_billing — sets up billing and returns a deterministic billingId.
 */
public class SetupBillingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "msc_setup_billing";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [msc_setup_billing] Setting up billing...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("billingId", "BILL-001");

        System.out.println("  [msc_setup_billing] Billing setup: BILL-001");
        return result;
    }
}
