package switchjavascript.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Common finalization step that runs after the SWITCH regardless of which branch was taken.
 */
public class FinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "swjs_finalize";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [finalize] Order finalized");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "swjs_finalize");
        result.getOutputData().put("processed", true);
        return result;
    }
}
