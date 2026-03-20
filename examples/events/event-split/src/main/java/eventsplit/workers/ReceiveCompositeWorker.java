package eventsplit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Receives a composite event and passes it through.
 * Input: compositeEvent (map)
 * Output: event (the compositeEvent)
 */
public class ReceiveCompositeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sp_receive_composite";
    }

    @Override
    public TaskResult execute(Task task) {
        Object compositeEvent = task.getInputData().get("compositeEvent");
        if (compositeEvent == null) {
            compositeEvent = Map.of();
        }

        System.out.println("  [sp_receive_composite] Composite event received");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("event", compositeEvent);
        return result;
    }
}
