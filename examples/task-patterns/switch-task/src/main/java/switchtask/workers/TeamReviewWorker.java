package switchtask.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles MEDIUM priority tickets by assigning to a support team.
 *
 * Output:
 * - handler (String): "team"
 * - assignedTo (String): "support-team-1"
 */
public class TeamReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sw_team_review";
    }

    @Override
    public TaskResult execute(Task task) {
        String ticketId = (String) task.getInputData().get("ticketId");
        String priority = (String) task.getInputData().get("priority");

        System.out.println("  [team_review] Ticket " + ticketId + " (priority: " + priority + ") -> assigned to support-team-1");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "team");
        result.getOutputData().put("assignedTo", "support-team-1");
        return result;
    }
}
