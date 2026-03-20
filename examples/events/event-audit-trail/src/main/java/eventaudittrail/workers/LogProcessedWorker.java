package eventaudittrail.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Logs that an event has been processed.
 * Input: eventId, processResult, stage
 * Output: logged (true), stage ("processed"), timestamp
 */
public class LogProcessedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "at_log_processed";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        Object processResult = task.getInputData().get("processResult");

        String stage = (String) task.getInputData().get("stage");
        if (stage == null) {
            stage = "processed";
        }

        System.out.println("  [at_log_processed] Logging processed event: " + eventId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        result.getOutputData().put("stage", "processed");
        result.getOutputData().put("timestamp", "2026-01-15T10:00:02Z");
        return result;
    }
}
