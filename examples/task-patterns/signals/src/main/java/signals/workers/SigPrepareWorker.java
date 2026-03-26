package signals.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Prepares the order for shipping.
 * Takes an orderId and returns { ready: true }.
 */
public class SigPrepareWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sig_prepare";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        if (orderId == null || orderId.isBlank()) {
            orderId = "unknown";
        }

        System.out.println("  [sig_prepare] Preparing order: " + orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orderId", orderId);
        result.getOutputData().put("ready", true);
        return result;
    }
}
