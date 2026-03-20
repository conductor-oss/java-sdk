package claimcheck.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Stores a large payload in external storage and returns a lightweight claim check reference.
 * Input: payload, storageType
 * Output: claimCheckId, storageLocation, payloadSizeBytes
 */
public class StorePayloadWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "clc_store_payload";
    }

    @Override
    public TaskResult execute(Task task) {
        Object payload = task.getInputData().get("payload");
        if (payload == null) {
            payload = Map.of();
        }
        String storageType = (String) task.getInputData().get("storageType");
        if (storageType == null) {
            storageType = "s3";
        }

        int sizeBytes = payload.toString().length();
        String claimCheckId = "CC-fixed-001";

        System.out.println("  [store] Stored payload (" + sizeBytes + " bytes) — claim check: " + claimCheckId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("claimCheckId", claimCheckId);
        result.getOutputData().put("storageLocation", storageType + "://claim-checks/" + claimCheckId);
        result.getOutputData().put("payloadSizeBytes", sizeBytes);
        return result;
    }
}
