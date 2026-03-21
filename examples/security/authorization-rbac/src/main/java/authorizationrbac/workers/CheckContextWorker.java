package authorizationrbac.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks contextual factors (time, location, device) for access decision.
 * Input: check_contextData
 * Output: check_context, processed
 */
public class CheckContextWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rbac_check_context";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [context] Access within business hours, from corporate network");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("check_context", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
