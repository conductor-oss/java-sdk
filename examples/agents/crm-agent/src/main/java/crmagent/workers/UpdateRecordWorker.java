package crmagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Updates the customer's CRM record with the new interaction.
 * Creates a ticket, increments the interaction count, and records a timestamp.
 */
public class UpdateRecordWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cm_update_record";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerId = (String) task.getInputData().get("customerId");
        if (customerId == null || customerId.isBlank()) {
            customerId = "UNKNOWN";
        }

        String inquiry = (String) task.getInputData().get("inquiry");
        if (inquiry == null || inquiry.isBlank()) {
            inquiry = "No inquiry provided";
        }

        String channel = (String) task.getInputData().get("channel");
        if (channel == null || channel.isBlank()) {
            channel = "unknown";
        }

        Object interactionCountObj = task.getInputData().get("interactionCount");
        int interactionCount = 0;
        if (interactionCountObj instanceof Number) {
            interactionCount = ((Number) interactionCountObj).intValue();
        }

        System.out.println("  [cm_update_record] Updating record for: " + customerId
                + " (channel=" + channel + ", interactions=" + interactionCount + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ticketId", "TKT-FIXED-001");
        result.getOutputData().put("recordUpdated", true);
        result.getOutputData().put("newInteractionCount", interactionCount + 1);
        result.getOutputData().put("timestamp", "2026-03-08T10:00:00Z");
        return result;
    }
}
