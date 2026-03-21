package eventversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Passes through an event that is already at the latest version.
 * Input: event (map)
 * Output: transformed (the event unchanged)
 */
public class PassThroughWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vr_pass_through";
    }

    @Override
    public TaskResult execute(Task task) {
        Object event = task.getInputData().get("event");
        if (event == null) {
            event = Map.of();
        }

        System.out.println("  [vr_pass_through] Event already at latest version");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformed", event);
        return result;
    }
}
