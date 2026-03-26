package azureintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Verifies that all Azure services completed successfully.
 * Input: blobUrl, cosmosId, eventHubSeq
 * Output: verified (boolean)
 */
public class AzVerifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "az_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        Object blobUrl = task.getInputData().get("blobUrl");
        Object cosmosId = task.getInputData().get("cosmosId");
        Object eventHubSeq = task.getInputData().get("eventHubSeq");

        boolean allPresent = blobUrl != null && !String.valueOf(blobUrl).isEmpty()
                && cosmosId != null && !String.valueOf(cosmosId).isEmpty()
                && eventHubSeq != null && !String.valueOf(eventHubSeq).isEmpty();

        System.out.println("  [verify] Blob: " + blobUrl + ", Cosmos: " + cosmosId + ", EventHub: " + eventHubSeq);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", allPresent);
        return result;
    }
}
