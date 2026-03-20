package switchdefaultcase.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Logs the payment processing action. Runs after the SWITCH for all cases.
 *
 * Output:
 * - logged (boolean): true
 */
public class LogWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dc_log";
    }

    @Override
    public TaskResult execute(Task task) {
        String paymentMethod = (String) task.getInputData().get("paymentMethod");

        System.out.println("  [log] Logged processing for payment method: " + paymentMethod);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        return result;
    }
}
