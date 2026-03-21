package tradeexecution.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends trade confirmation to the client.
 */
public class ConfirmWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "trd_confirm";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        Object fillPrice = task.getInputData().get("fillPrice");

        System.out.println("  [confirm] Trade confirmation sent for " + orderId + " — fill @ $" + fillPrice);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("confirmed", true);
        result.getOutputData().put("confirmationId", "CONF-TRD-88201");
        result.getOutputData().put("settlesOn", "T+1");
        return result;
    }
}
