package eventfanout.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Receives an incoming event and passes it through for fan-out processing.
 * Input: eventId, eventType, payload
 * Output: eventId, eventType, payload
 */
public class ReceiveEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fo_receive_event";
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

        Object payload = task.getInputData().get("payload");
        if (payload == null) {
            payload = Map.of();
        }

        System.out.println("  [fo_receive_event] Received event: " + eventId + " type: " + eventType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("eventId", eventId);
        result.getOutputData().put("eventType", eventType);
        result.getOutputData().put("payload", payload);
        return result;
    }
}
