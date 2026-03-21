package switchjavascript.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles VIP customers with standard-value orders.
 */
public class VipStandardWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "swjs_vip_standard";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [vip_standard] VIP standard order handled");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "swjs_vip_standard");
        result.getOutputData().put("processed", true);
        return result;
    }
}
