package digitalassetmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Stores the tagged and versioned asset in the DAM repository.
 * Input: assetId, storagePath, tags, version
 * Output: assetUrl, storedAt, indexed
 */
public class StoreAssetWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dam_store_asset";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String version = (String) task.getInputData().get("version");
        if (version == null) version = "unknown";

        Object tagsObj = task.getInputData().get("tags");
        int tagCount = 0;
        if (tagsObj instanceof List) {
            tagCount = ((List<?>) tagsObj).size();
        }

        System.out.println("  [store] Storing asset v" + version + " with " + tagCount + " tags");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assetUrl", "https://dam.example.com/assets/521/v1.0.0");
        result.getOutputData().put("storedAt", "2026-03-08T10:00:00Z");
        result.getOutputData().put("indexed", true);
        return result;
    }
}
