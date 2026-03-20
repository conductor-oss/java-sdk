package eventhandlers.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes an incoming event.
 * Takes an eventType and payload, returns a result message confirming processing.
 */
public class ProcessEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eh_process_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null || eventType.isBlank()) {
            eventType = "unknown";
        }

        Object payload = task.getInputData().get("payload");
        if (payload == null) {
            payload = "{}";
        }

        System.out.println("  [eh_process_event] Processing " + eventType + " event with payload: " + payload);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "Processed " + eventType + " event");
        result.getOutputData().put("eventType", eventType);
        result.getOutputData().put("payload", payload);
        return result;
    }
}
