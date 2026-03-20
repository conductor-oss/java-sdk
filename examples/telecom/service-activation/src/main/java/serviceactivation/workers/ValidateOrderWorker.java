package serviceactivation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ValidateOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sac_validate_order";
    }

    @Override
    public TaskResult execute(Task task) {

        String orderId = (String) task.getInputData().get("orderId");
        System.out.printf("  [validate] Order %s validated%n", orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", true);
        result.getOutputData().put("paymentVerified", true);
        return result;
    }
}
