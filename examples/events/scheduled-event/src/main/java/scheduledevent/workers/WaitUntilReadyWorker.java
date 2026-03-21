package scheduledevent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Waits until the scheduled event is ready for execution.
 * Input: delayMs, eventId
 * Output: waited (true)
 */
public class WaitUntilReadyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "se_wait_until_ready";
    }

    @Override
    public TaskResult execute(Task task) {
        Object delayMsObj = task.getInputData().get("delayMs");
        int delayMs = 0;
        if (delayMsObj instanceof Number) {
            delayMs = ((Number) delayMsObj).intValue();
        }

        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        System.out.println("  [se_wait_until_ready] Waiting " + delayMs + "ms for event: " + eventId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("waited", true);
        return result;
    }
}
