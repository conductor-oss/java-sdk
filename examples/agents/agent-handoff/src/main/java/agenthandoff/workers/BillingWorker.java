package agenthandoff.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Billing specialist agent — handles billing-related customer issues,
 * processes refund requests, and returns resolution details. The ticket ID
 * and actions are derived deterministically from the customer ID and message.
 */
public class BillingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ah_billing";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerId = (String) task.getInputData().get("customerId");
        String message = (String) task.getInputData().get("message");
        String triageNotes = (String) task.getInputData().get("triageNotes");
        String urgency = (String) task.getInputData().get("urgency");

        if (customerId == null || customerId.isBlank()) {
            customerId = "unknown";
        }
        if (message == null || message.isBlank()) {
            message = "";
        }
        if (triageNotes == null) {
            triageNotes = "";
        }
        if (urgency == null || urgency.isBlank()) {
            urgency = "normal";
        }

        System.out.println("  [ah_billing] Handling billing issue for customer " + customerId);

        // Deterministic ticket ID based on customer ID hash.
        int ticketNum = Math.abs(customerId.hashCode() % 9000) + 1000;
        String ticketId = "BIL-" + ticketNum;

        // Determine resolution and actions based on message content.
        String lowerMessage = message.toLowerCase();
        String resolution;
        List<String> actions = new ArrayList<>();

        if (lowerMessage.contains("refund") || lowerMessage.contains("overcharged") || lowerMessage.contains("charged twice")) {
            resolution = "Reviewed charge history for " + customerId + " and initiated a refund for the disputed amount. Credit will appear within 5-7 business days.";
            actions.add("refund_initiated");
            actions.add("email_confirmation_sent");
        } else if (lowerMessage.contains("invoice") || lowerMessage.contains("receipt")) {
            resolution = "Located invoice records for " + customerId + " and sent copies to the email on file.";
            actions.add("invoice_resent");
            actions.add("email_confirmation_sent");
        } else {
            resolution = "Reviewed billing concern for " + customerId + ". Account is current; no discrepancies found. Summary sent to customer.";
            actions.add("account_reviewed");
            actions.add("summary_sent");
        }

        if ("high".equals(urgency)) {
            actions.add("case_flagged_for_review");
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("specialist", "billing");
        output.put("ticketId", ticketId);
        output.put("resolution", resolution);
        output.put("actions", actions);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().putAll(output);
        return result;
    }
}
