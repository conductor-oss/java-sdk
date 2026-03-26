package digitalassetmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Ingests a digital asset from the source URL and stores the original.
 * Input: assetId, assetType, sourceUrl
 * Output: storagePath, fileSizeMb, checksum, dimensions
 */
public class IngestAssetWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dam_ingest_asset";
    }

    @Override
    public TaskResult execute(Task task) {
        String assetId = (String) task.getInputData().get("assetId");
        String assetType = (String) task.getInputData().get("assetType");
        if (assetId == null) assetId = "unknown";
        if (assetType == null) assetType = "unknown";

        System.out.println("  [ingest] Ingesting " + assetType + " asset " + assetId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("storagePath", "s3://dam/assets/521/original.psd");
        result.getOutputData().put("fileSizeMb", 85);
        result.getOutputData().put("checksum", "sha256:abc123def456");
        result.getOutputData().put("dimensions", "4000x3000");
        return result;
    }
}
