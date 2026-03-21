package eventdrivenworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Handles generic (unclassified) events.
 * Input: eventData, category
 * Output: handler ("generic"), processed (true), category
 */
public class HandleGenericWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ed_handle_generic";
    }

    @Override
    public TaskResult execute(Task task) {
        String category = (String) task.getInputData().get("category");
        if (category == null) {
            category = "generic";
        }

        System.out.println("  [ed_handle_generic] Processing generic event (category: " + category + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "generic");
        result.getOutputData().put("processed", true);
        result.getOutputData().put("category", category);
        return result;
    }
}
