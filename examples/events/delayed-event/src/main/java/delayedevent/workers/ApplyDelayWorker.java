package delayedevent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Applies the computed delay (deterministic. instant in mock).
 * Input: delayMs, eventId
 * Output: delayed (true), actualDelayMs (same as delayMs)
 */
public class ApplyDelayWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "de_apply_delay";
    }

    @Override
    public TaskResult execute(Task task) {
        Object delayMs = task.getInputData().get("delayMs");
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        System.out.println("  [de_apply_delay] Applying " + delayMs + "ms delay for event " + eventId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("delayed", true);
        result.getOutputData().put("actualDelayMs", delayMs);
        return result;
    }
}
