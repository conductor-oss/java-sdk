package switchdefaultcase.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes credit card payments via Stripe.
 *
 * Output:
 * - handler (String): "stripe"
 */
public class ProcessCardWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dc_process_card";
    }

    @Override
    public TaskResult execute(Task task) {
        String paymentMethod = (String) task.getInputData().get("paymentMethod");

        System.out.println("  [process_card] Payment method: " + paymentMethod + " -> handler: stripe");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "stripe");
        return result;
    }
}
