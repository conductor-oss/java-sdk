package supplychainmgmt.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Configures the return policy for a delivery.
 * Input: deliveryId
 * Output: returnPolicy, active
 */
public class ReturnWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "scm_return";
    }

    @Override
    public TaskResult execute(Task task) {
        String deliveryId = (String) task.getInputData().get("deliveryId");

        System.out.println("  [return] Return policy configured for " + deliveryId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("returnPolicy", "30-day return window");
        result.getOutputData().put("active", true);
        return result;
    }
}
