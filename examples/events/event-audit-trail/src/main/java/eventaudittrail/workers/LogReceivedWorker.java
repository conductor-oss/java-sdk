package eventaudittrail.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Logs that an event has been received.
 * Input: eventId, eventType, stage
 * Output: logged (true), stage ("received"), timestamp
 */
public class LogReceivedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "at_log_received";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null) {
            eventType = "unknown";
        }

        String stage = (String) task.getInputData().get("stage");
        if (stage == null) {
            stage = "received";
        }

        System.out.println("  [at_log_received] Logging received event: " + eventId + " type=" + eventType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        result.getOutputData().put("stage", "received");
        result.getOutputData().put("timestamp", "2026-01-15T10:00:00Z");
        return result;
    }
}
