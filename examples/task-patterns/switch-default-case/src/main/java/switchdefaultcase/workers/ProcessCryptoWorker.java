package switchdefaultcase.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes crypto payments via Coinbase.
 *
 * Output:
 * - handler (String): "coinbase"
 */
public class ProcessCryptoWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dc_process_crypto";
    }

    @Override
    public TaskResult execute(Task task) {
        String paymentMethod = (String) task.getInputData().get("paymentMethod");

        System.out.println("  [process_crypto] Payment method: " + paymentMethod + " -> handler: coinbase");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "coinbase");
        return result;
    }
}
