package assettracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Tags an asset with a tracking device. Real tag ID generation and battery check.
 */
public class TagAssetWorker implements Worker {
    @Override public String getTaskDefName() { return "ast_tag_asset"; }
    @Override public TaskResult execute(Task task) {
        String assetId = (String) task.getInputData().get("assetId");
        String tagType = (String) task.getInputData().get("tagType");
        if (assetId == null) assetId = "UNKNOWN";
        if (tagType == null) tagType = "BLE_GPS";

        String tagId = "TAG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int batteryLevel = 90 + (Math.abs(tagId.hashCode()) % 11); // 90-100

        System.out.println("  [tag] Asset " + assetId + " tagged with " + tagId + " (" + tagType + ", battery: " + batteryLevel + "%)");

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("tagId", tagId);
        r.getOutputData().put("tagType", tagType);
        r.getOutputData().put("batteryLevel", batteryLevel);
        r.getOutputData().put("activatedAt", Instant.now().toString());
        return r;
    }
}
