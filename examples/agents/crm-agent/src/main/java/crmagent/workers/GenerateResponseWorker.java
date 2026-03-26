package crmagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Generates a personalized response email for the customer based on their
 * profile, tier, inquiry, recent issues, and sentiment.
 */
public class GenerateResponseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cm_generate_response";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String customerName = (String) task.getInputData().get("customerName");
        if (customerName == null || customerName.isBlank()) {
            customerName = "Valued Customer";
        }

        String tier = (String) task.getInputData().get("tier");
        if (tier == null || tier.isBlank()) {
            tier = "standard";
        }

        String inquiry = (String) task.getInputData().get("inquiry");
        if (inquiry == null || inquiry.isBlank()) {
            inquiry = "General inquiry";
        }

        List<Map<String, Object>> recentIssues =
                (List<Map<String, Object>>) task.getInputData().get("recentIssues");

        String sentiment = (String) task.getInputData().get("sentiment");
        if (sentiment == null || sentiment.isBlank()) {
            sentiment = "neutral";
        }

        System.out.println("  [cm_generate_response] Generating response for: " + customerName
                + " (tier=" + tier + ", sentiment=" + sentiment + ")");

        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(customerName).append(",\n\n");
        sb.append("Thank you for reaching out to us regarding your inquiry:\n");
        sb.append("\"").append(inquiry).append("\"\n\n");
        sb.append("As a valued ").append(tier).append(" customer, your request has been ");
        sb.append("prioritized and assigned to our senior engineering team.\n\n");

        if (recentIssues != null && !recentIssues.isEmpty()) {
            sb.append("We also wanted to follow up on your recent interactions:\n");
            for (Map<String, Object> issue : recentIssues) {
                String subject = (String) issue.getOrDefault("subject", "Unknown");
                String status = (String) issue.getOrDefault("status", "unknown");
                sb.append("  - ").append(subject).append(" (").append(status).append(")\n");
            }
            sb.append("\n");
        }

        sb.append("Your dedicated account representative will be in touch shortly ");
        sb.append("with a detailed resolution plan.\n\n");
        sb.append("Best regards,\n");
        sb.append("Customer Success Team");

        String priority = "enterprise".equals(tier) ? "high" : "normal";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("response", sb.toString());
        result.getOutputData().put("priority", priority);
        result.getOutputData().put("responseType", "personalized");
        result.getOutputData().put("estimatedResolution", "4 hours");
        result.getOutputData().put("escalated", false);
        return result;
    }
}
