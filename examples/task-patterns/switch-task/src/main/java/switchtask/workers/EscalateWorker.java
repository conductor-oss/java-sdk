package switchtask.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles HIGH priority tickets by escalating to a manager.
 *
 * Output:
 * - handler (String): "manager"
 * - escalatedTo (String): "manager@example.com"
 */
public class EscalateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sw_escalate";
    }

    @Override
    public TaskResult execute(Task task) {
        String ticketId = (String) task.getInputData().get("ticketId");
        String priority = (String) task.getInputData().get("priority");

        System.out.println("  [escalate] Ticket " + ticketId + " (priority: " + priority + ") -> escalated to manager@example.com");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "manager");
        result.getOutputData().put("escalatedTo", "manager@example.com");
        return result;
    }
}
