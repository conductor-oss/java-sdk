package deadletterevents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Sends an alert notification when an event is routed to the dead letter queue.
 */
public class DlSendAlertWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dl_send_alert";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        String dlqId = (String) task.getInputData().get("dlqId");
        String errorReason = (String) task.getInputData().get("errorReason");

        System.out.println("  [dl_send_alert] Sending alert for event: " + eventId + " dlqId=" + dlqId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("alertId", "alert-fixed-001");
        result.getOutputData().put("alertSent", true);
        result.getOutputData().put("recipients", List.of("ops-team@company.com"));
        result.getOutputData().put("sentAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
