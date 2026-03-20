package ticketmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class AssignTicketWorker implements Worker {
    @Override public String getTaskDefName() { return "tkt_assign"; }

    @Override
    public TaskResult execute(Task task) {
        String category = (String) task.getInputData().getOrDefault("category", "general");
        Map<String, String> teams = Map.of("authentication", "Auth Team - Kim", "performance", "Infra Team - Leo", "general", "Support - Maria");
        String assignee = teams.getOrDefault(category, "Unassigned");
        String priority = (String) task.getInputData().getOrDefault("priority", "P2");
        System.out.println("  [assign] " + task.getInputData().get("ticketId") + " assigned to " + assignee);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assignee", assignee);
        result.getOutputData().put("slaHours", "P1".equals(priority) ? 4 : 24);
        return result;
    }
}
