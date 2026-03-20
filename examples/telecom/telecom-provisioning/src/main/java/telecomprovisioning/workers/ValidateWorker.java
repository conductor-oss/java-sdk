package telecomprovisioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ValidateWorker implements Worker {
    @Override public String getTaskDefName() { return "tpv_validate"; }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        String planId = (String) task.getInputData().get("planId");
        System.out.printf("  [validate] Order %s validated — plan %s%n", orderId, planId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", true);
        result.getOutputData().put("creditCheck", "passed");
        return result;
    }
}
