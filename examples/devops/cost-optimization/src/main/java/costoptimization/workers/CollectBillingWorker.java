package costoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectBillingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "co_collect_billing";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [billing] Collected billing data for prod-account-001");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("collect_billingId", "COLLECT_BILLING-1349");
        result.addOutputData("success", true);
        return result;
    }
}
