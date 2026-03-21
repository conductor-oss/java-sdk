package delayedevent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Computes the delay duration in milliseconds from delaySeconds.
 * Input: delaySeconds, eventId
 * Output: delayMs (parseInt(delaySeconds)*1000 or 5000), delaySeconds
 */
public class ComputeDelayWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "de_compute_delay";
    }

    @Override
    public TaskResult execute(Task task) {
        Object delaySecondsRaw = task.getInputData().get("delaySeconds");
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        int delaySeconds;
        try {
            delaySeconds = Integer.parseInt(String.valueOf(delaySecondsRaw));
        } catch (Exception e) {
            delaySeconds = 5;
        }

        int delayMs = delaySeconds * 1000;

        System.out.println("  [de_compute_delay] Computed delay: " + delayMs + "ms for event " + eventId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("delayMs", delayMs);
        result.getOutputData().put("delaySeconds", delaySecondsRaw);
        return result;
    }
}
