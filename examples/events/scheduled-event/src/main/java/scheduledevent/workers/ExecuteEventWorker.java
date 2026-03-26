package scheduledevent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Executes the scheduled event.
 * Input: eventId, payload
 * Output: executedAt ("2026-01-15T10:00:00Z"), result ("success")
 */
public class ExecuteEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "se_execute_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        Object payload = task.getInputData().get("payload");
        if (payload == null) {
            payload = Map.of();
        }

        System.out.println("  [se_execute_event] Executing event: " + eventId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("executedAt", "2026-01-15T10:00:00Z");
        result.getOutputData().put("result", "success");
        return result;
    }
}
