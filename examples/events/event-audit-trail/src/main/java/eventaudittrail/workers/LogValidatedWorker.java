package eventaudittrail.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Logs that an event has been validated.
 * Input: eventId, validationResult, stage
 * Output: logged (true), stage ("validated"), timestamp
 */
public class LogValidatedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "at_log_validated";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        Object validationResult = task.getInputData().get("validationResult");

        String stage = (String) task.getInputData().get("stage");
        if (stage == null) {
            stage = "validated";
        }

        System.out.println("  [at_log_validated] Logging validated event: " + eventId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        result.getOutputData().put("stage", "validated");
        result.getOutputData().put("timestamp", "2026-01-15T10:00:01Z");
        return result;
    }
}
