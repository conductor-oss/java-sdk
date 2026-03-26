package eventfanout.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Tracks event analytics and updates metrics.
 * Input: eventId, payload
 * Output: result:"tracked", metricsUpdated:true
 */
public class AnalyticsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fo_analytics";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        System.out.println("  [fo_analytics] Tracking analytics for event: " + eventId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "tracked");
        result.getOutputData().put("metricsUpdated", true);
        return result;
    }
}
