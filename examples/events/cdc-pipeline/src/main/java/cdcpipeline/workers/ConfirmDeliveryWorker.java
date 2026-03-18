package cdcpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Confirms that all published CDC messages were successfully delivered.
 * Returns a deterministic delivery report.
 */
public class ConfirmDeliveryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cd_confirm_delivery";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String publishResult = (String) task.getInputData().get("publishResult");
        if (publishResult == null || publishResult.isBlank()) {
            publishResult = "unknown";
        }

        List<String> messageIds = (List<String>) task.getInputData().get("messageIds");
        if (messageIds == null) {
            messageIds = List.of();
        }

        System.out.println("  [cd_confirm_delivery] Confirming delivery of " + messageIds.size() + " messages");

        int total = messageIds.size();

        Map<String, Object> deliveryReport = Map.of(
                "total", total,
                "delivered", total,
                "failed", 0
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("allDelivered", true);
        result.getOutputData().put("confirmedAt", "2026-01-15T10:00:00Z");
        result.getOutputData().put("deliveryReport", deliveryReport);
        return result;
    }
}
