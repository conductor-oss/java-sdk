package switchtask.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles tickets with an unrecognized priority (default case).
 *
 * Output:
 * - handler (String): "default"
 * - needsClassification (boolean): true
 */
public class UnknownPriorityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sw_unknown_priority";
    }

    @Override
    public TaskResult execute(Task task) {
        String ticketId = (String) task.getInputData().get("ticketId");
        String priority = (String) task.getInputData().get("priority");

        System.out.println("  [unknown_priority] Ticket " + ticketId + " (priority: " + priority + ") -> needs classification");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "default");
        result.getOutputData().put("needsClassification", true);
        return result;
    }
}
