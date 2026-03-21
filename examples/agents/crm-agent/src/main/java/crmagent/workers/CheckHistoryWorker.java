package crmagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Checks the interaction history for a customer, returning recent issues,
 * total interactions, average satisfaction, sentiment, last contact date,
 * and open ticket count.
 */
public class CheckHistoryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cm_check_history";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerId = (String) task.getInputData().get("customerId");
        if (customerId == null || customerId.isBlank()) {
            customerId = "UNKNOWN";
        }

        String customerTier = (String) task.getInputData().get("customerTier");
        if (customerTier == null || customerTier.isBlank()) {
            customerTier = "standard";
        }

        String accountAge = (String) task.getInputData().get("accountAge");
        if (accountAge == null || accountAge.isBlank()) {
            accountAge = "unknown";
        }

        System.out.println("  [cm_check_history] Checking history for: " + customerId
                + " (tier=" + customerTier + ", age=" + accountAge + ")");

        List<Map<String, Object>> recentIssues = List.of(
                Map.of("date", "2026-02-15", "type", "technical",
                        "subject", "API rate limiting errors", "status", "resolved",
                        "satisfaction", 5),
                Map.of("date", "2026-01-22", "type", "billing",
                        "subject", "Invoice discrepancy Q4", "status", "resolved",
                        "satisfaction", 4),
                Map.of("date", "2026-01-08", "type", "feature_request",
                        "subject", "Bulk export endpoint", "status", "in_progress",
                        "satisfaction", 3)
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recentIssues", recentIssues);
        result.getOutputData().put("totalInteractions", 47);
        result.getOutputData().put("avgSatisfaction", 4.2);
        result.getOutputData().put("sentiment", "positive");
        result.getOutputData().put("lastContact", "2026-02-28");
        result.getOutputData().put("openTickets", 1);
        return result;
    }
}
