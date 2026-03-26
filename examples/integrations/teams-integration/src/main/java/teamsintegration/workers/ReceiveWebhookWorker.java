package teamsintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Receives a Teams webhook and extracts event data.
 * Input: teamId, channelId, payload
 * Output: eventType, data, receivedAt
 */
public class ReceiveWebhookWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tms_receive_webhook";
    }

    @Override
    public TaskResult execute(Task task) {
        String teamId = (String) task.getInputData().get("teamId");
        if (teamId == null) {
            teamId = "unknown-team";
        }
        Object payload = task.getInputData().get("payload");

        System.out.println("  [webhook] Received webhook for team " + teamId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("eventType", "alert");
        result.getOutputData().put("data", payload);
        result.getOutputData().put("receivedAt", "2025-01-15T10:30:00Z");
        return result;
    }
}
