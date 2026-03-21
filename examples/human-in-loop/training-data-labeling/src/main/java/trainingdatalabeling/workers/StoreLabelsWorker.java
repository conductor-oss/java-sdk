package trainingdatalabeling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for tdl_store_labels task -- stores the final labeled data.
 *
 * This is the last step in the training data labeling workflow.
 * After agreement has been computed, the labels are stored.
 * Returns stored=true to indicate success.
 */
public class StoreLabelsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tdl_store_labels";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [tdl_store_labels] Storing labeled data...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("stored", true);

        System.out.println("  [tdl_store_labels] Labels stored.");
        return result;
    }
}
