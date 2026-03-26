package switchdefaultcase.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles unrecognized payment methods (default case).
 * Takes the method name and flags it for manual review.
 *
 * Output:
 * - handler (String): "manual_review"
 * - needsHuman (boolean): true
 */
public class UnknownMethodWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dc_unknown_method";
    }

    @Override
    public TaskResult execute(Task task) {
        String method = (String) task.getInputData().get("method");

        System.out.println("  [unknown_method] Unrecognized method: " + method + " -> manual_review (needsHuman: true)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "manual_review");
        result.getOutputData().put("needsHuman", true);
        return result;
    }
}
