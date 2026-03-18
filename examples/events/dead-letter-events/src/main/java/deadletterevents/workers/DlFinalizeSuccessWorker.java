package deadletterevents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Finalizes a successfully processed event by stamping a finalized timestamp.
 */
public class DlFinalizeSuccessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dl_finalize_success";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        Object resultData = task.getInputData().get("result");

        System.out.println("  [dl_finalize_success] Finalizing event: " + eventId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalized", true);
        result.getOutputData().put("finalizedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
