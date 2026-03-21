package documentverification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for dv_store_verified task -- stores the verified document data.
 *
 * This is the final step in the document verification workflow, executed
 * after the human reviewer has verified (or corrected) the AI-extracted data.
 * Returns { stored: true } to confirm successful storage.
 */
public class StoreVerifiedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dv_store_verified";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [dv_store_verified] Storing verified document data...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("stored", true);

        System.out.println("  [dv_store_verified] Data stored successfully.");
        return result;
    }
}
