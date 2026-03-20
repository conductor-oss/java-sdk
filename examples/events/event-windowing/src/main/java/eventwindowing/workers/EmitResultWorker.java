package eventwindowing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Emits the final windowed result, confirming the window was successfully
 * processed and published.
 */
public class EmitResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ew_emit_result";
    }

    @Override
    public TaskResult execute(Task task) {
        String windowId = (String) task.getInputData().get("windowId");
        if (windowId == null || windowId.isBlank()) {
            windowId = "win_fixed_001";
        }

        System.out.println("  [ew_emit_result] Emitting result for window: " + windowId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("emitted", true);
        result.getOutputData().put("windowId", windowId);
        return result;
    }
}
