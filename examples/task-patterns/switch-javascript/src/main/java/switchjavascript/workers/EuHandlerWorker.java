package switchjavascript.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles orders from the EU region for compliance processing.
 */
public class EuHandlerWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "swjs_eu_handler";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [eu_handler] EU compliance processing applied");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "swjs_eu_handler");
        result.getOutputData().put("processed", true);
        return result;
    }
}
