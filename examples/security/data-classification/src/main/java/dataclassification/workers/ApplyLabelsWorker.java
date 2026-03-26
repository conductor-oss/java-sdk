package dataclassification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Applies classification labels to data catalog.
 * Input: apply_labelsData (from classify)
 * Output: apply_labels, completedAt
 */
public class ApplyLabelsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dc_apply_labels";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [label] Classification labels applied to data catalog");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("apply_labels", true);
        result.getOutputData().put("completedAt", "2026-01-15T10:05:00Z");
        return result;
    }
}
