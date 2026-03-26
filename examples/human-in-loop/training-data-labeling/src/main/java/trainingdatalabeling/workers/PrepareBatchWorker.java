package trainingdatalabeling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for tdl_prepare_batch task -- prepares a batch of data items for labeling.
 *
 * This is the first step in the training data labeling workflow.
 * It takes the batch input and returns prepared=true to indicate
 * the batch is ready for annotators.
 */
public class PrepareBatchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tdl_prepare_batch";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [tdl_prepare_batch] Preparing batch for labeling...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("prepared", true);

        System.out.println("  [tdl_prepare_batch] Batch prepared.");
        return result;
    }
}
