package switchjavascript.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles VIP customers with high-value orders (amount > 1000).
 */
public class VipConciergeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "swjs_vip_concierge";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [vip_concierge] VIP high-value order handled");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "swjs_vip_concierge");
        result.getOutputData().put("processed", true);
        return result;
    }
}
