package switchdefaultcase.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes bank transfer payments via Plaid.
 *
 * Output:
 * - handler (String): "plaid"
 */
public class ProcessBankWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dc_process_bank";
    }

    @Override
    public TaskResult execute(Task task) {
        String paymentMethod = (String) task.getInputData().get("paymentMethod");

        System.out.println("  [process_bank] Payment method: " + paymentMethod + " -> handler: plaid");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "plaid");
        return result;
    }
}
