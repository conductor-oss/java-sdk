package switchtask.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles LOW priority tickets automatically.
 *
 * Output:
 * - handler (String): "auto"
 * - resolved (boolean): true
 */
public class AutoHandleWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sw_auto_handle";
    }

    @Override
    public TaskResult execute(Task task) {
        String ticketId = (String) task.getInputData().get("ticketId");
        String priority = (String) task.getInputData().get("priority");

        System.out.println("  [auto_handle] Ticket " + ticketId + " (priority: " + priority + ") -> auto-resolved");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "auto");
        result.getOutputData().put("resolved", true);
        return result;
    }
}
