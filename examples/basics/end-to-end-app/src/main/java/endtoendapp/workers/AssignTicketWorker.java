package endtoendapp.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Assigns a support ticket to the appropriate team based on category and priority.
 * Upgrades response time for CRITICAL and HIGH priority tickets.
 */
public class AssignTicketWorker implements Worker {

    private static final Map<String, String> CATEGORY_TEAMS = Map.of(
            "billing", "Finance Support",
            "technical", "Engineering",
            "account", "Account Management",
            "general", "General Support"
    );

    private static final Map<String, String> CATEGORY_RESPONSE_TIMES = Map.of(
            "billing", "4 hours",
            "technical", "2 hours",
            "account", "8 hours",
            "general", "24 hours"
    );

    @Override
    public String getTaskDefName() {
        return "assign_ticket";
    }

    @Override
    public TaskResult execute(Task task) {
        String ticketId = (String) task.getInputData().get("ticketId");
        String category = (String) task.getInputData().get("category");
        String priority = (String) task.getInputData().get("priority");

        String normalizedCategory = (category != null) ? category.toLowerCase() : "general";
        String team = CATEGORY_TEAMS.getOrDefault(normalizedCategory, "General Support");
        String estimatedResponse = CATEGORY_RESPONSE_TIMES.getOrDefault(normalizedCategory, "24 hours");

        // Upgrade response time for high-priority tickets
        if ("CRITICAL".equals(priority)) {
            estimatedResponse = "30 minutes";
        } else if ("HIGH".equals(priority)) {
            estimatedResponse = "1 hour";
        }

        System.out.println("  [assign_ticket] Ticket " + ticketId + " assigned to " + team
                + " (response: " + estimatedResponse + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("team", team);
        result.getOutputData().put("estimatedResponse", estimatedResponse);
        return result;
    }
}
