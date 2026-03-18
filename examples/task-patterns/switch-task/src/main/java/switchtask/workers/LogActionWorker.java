package switchtask.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Logs the action taken on a ticket. Runs after the SWITCH for all cases.
 *
 * Output:
 * - logged (boolean): true
 */
public class LogActionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sw_log_action";
    }

    @Override
    public TaskResult execute(Task task) {
        String ticketId = (String) task.getInputData().get("ticketId");
        String priority = (String) task.getInputData().get("priority");

        System.out.println("  [log_action] Logged action for ticket " + ticketId + " (priority: " + priority + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        return result;
    }
}
