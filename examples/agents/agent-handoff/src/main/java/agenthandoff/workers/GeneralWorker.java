package agenthandoff.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * General support agent — handles requests that do not fall into billing
 * or technical categories. Provides plan information, account details,
 * and follow-up actions based on message content.
 */
public class GeneralWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ah_general";
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

        System.out.println("  [ah_general] Handling general inquiry for customer " + customerId);

        // Deterministic ticket ID based on customer ID hash.
        int ticketNum = Math.abs(customerId.hashCode() % 9000) + 1000;
        String ticketId = "GEN-" + ticketNum;

        // Determine resolution and actions based on message content.
        String lowerMessage = message.toLowerCase();
        String resolution;
        List<String> actions = new ArrayList<>();

        if (lowerMessage.contains("plan") || lowerMessage.contains("upgrade") || lowerMessage.contains("pricing")) {
            resolution = "Provided plan comparison and pricing details to " + customerId + ". Recommended the Pro plan based on current usage patterns.";
            actions.add("plan_info_sent");
            actions.add("upgrade_recommendation");
        } else if (lowerMessage.contains("cancel") || lowerMessage.contains("close")) {
            resolution = "Acknowledged cancellation inquiry from " + customerId + ". Scheduled a retention callback to discuss options before processing.";
            actions.add("retention_callback_scheduled");
        } else {
            resolution = "Reviewed general inquiry from " + customerId + " and provided a detailed response. Follow-up scheduled if further assistance is needed.";
            actions.add("inquiry_answered");
            actions.add("follow_up_scheduled");
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("specialist", "general");
        output.put("ticketId", ticketId);
        output.put("resolution", resolution);
        output.put("actions", actions);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().putAll(output);
        return result;
    }
}
