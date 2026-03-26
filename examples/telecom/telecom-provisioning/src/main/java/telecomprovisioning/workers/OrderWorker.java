package telecomprovisioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class OrderWorker implements Worker {
    @Override public String getTaskDefName() { return "tpv_order"; }

    @Override
    public TaskResult execute(Task task) {
        String customerId = (String) task.getInputData().get("customerId");
        System.out.printf("  [order] Service order placed for customer %s%n", customerId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orderId", "ORD-telecom-provisioning-001");
        return result;
    }
}
