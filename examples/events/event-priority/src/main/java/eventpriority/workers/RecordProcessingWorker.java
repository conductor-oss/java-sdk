package eventpriority.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Records the processing result for auditing.
 * Input: eventId, priority
 * Output: recorded (true)
 */
public class RecordProcessingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pr_record_processing";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        String priority = (String) task.getInputData().get("priority");
        if (priority == null) {
            priority = "unknown";
        }

        System.out.println("  [pr_record_processing] Recording event " + eventId + " with priority " + priority);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recorded", true);
        return result;
    }
}
