package zendeskintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Routes ticket to an agent.
 * Input: ticketId, priority, category
 * Output: agentId, agentName, group
 */
public class RouteTicketWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "zd_route";
    }

    @Override
    public TaskResult execute(Task task) {
        String ticketId = (String) task.getInputData().get("ticketId");
        String priority = (String) task.getInputData().get("priority");
        System.out.println("  [route] Ticket " + ticketId + " (" + priority + ") -> Mike Chen");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("agentId", "agent-007");
        result.getOutputData().put("agentName", "Mike Chen");
        result.getOutputData().put("group", "tier-1-support");
        return result;
    }
}
