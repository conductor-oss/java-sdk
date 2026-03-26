package switchjavascript.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Default handler for orders that don't match any special criteria.
 */
public class StandardWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "swjs_standard";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [standard] Standard order processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "swjs_standard");
        result.getOutputData().put("processed", true);
        return result;
    }
}
