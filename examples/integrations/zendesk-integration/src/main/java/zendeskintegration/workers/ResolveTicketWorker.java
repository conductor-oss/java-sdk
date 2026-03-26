package zendeskintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Resolves a ticket.
 * Input: ticketId, agentId
 * Output: resolved, resolvedAt, satisfaction
 */
public class ResolveTicketWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "zd_resolve";
    }

    @Override
    public TaskResult execute(Task task) {
        String ticketId = (String) task.getInputData().get("ticketId");
        String agentId = (String) task.getInputData().get("agentId");
        System.out.println("  [resolve] Ticket " + ticketId + " resolved by " + agentId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("resolved", true);
        result.getOutputData().put("resolvedAt", "" + java.time.Instant.now().toString());
        result.getOutputData().put("satisfaction", "good");
        return result;
    }
}
