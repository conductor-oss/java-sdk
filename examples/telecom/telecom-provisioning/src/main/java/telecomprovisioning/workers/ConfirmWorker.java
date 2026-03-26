package telecomprovisioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ConfirmWorker implements Worker {
    @Override public String getTaskDefName() { return "tpv_confirm"; }

    @Override
    public TaskResult execute(Task task) {
        String customerId = (String) task.getInputData().get("customerId");
        String serviceId = (String) task.getInputData().get("serviceId");
        System.out.printf("  [confirm] Customer %s notified — service %s live%n", customerId, serviceId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("provisionStatus", "active");
        result.getOutputData().put("confirmed", true);
        return result;
    }
}
