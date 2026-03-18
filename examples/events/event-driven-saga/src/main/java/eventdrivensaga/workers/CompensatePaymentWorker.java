package eventdrivensaga.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Compensates (refunds) a payment when the saga needs to roll back.
 * Input: orderId, transactionId
 * Output: refunded (true), transactionId
 */
public class CompensatePaymentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ds_compensate_payment";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        if (orderId == null) {
            orderId = "UNKNOWN";
        }

        String transactionId = (String) task.getInputData().get("transactionId");
        if (transactionId == null) {
            transactionId = "UNKNOWN";
        }

        System.out.println("  [ds_compensate_payment] Refunding transaction " + transactionId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("refunded", true);
        result.getOutputData().put("transactionId", transactionId);
        return result;
    }
}
