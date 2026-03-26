package oauthtokenmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Stores token metadata for revocation support.
 * Input: store_tokenData
 * Output: store_token, processed
 */
public class StoreTokenWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "otm_store_token";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [store] Token metadata stored for revocation support");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("store_token", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
