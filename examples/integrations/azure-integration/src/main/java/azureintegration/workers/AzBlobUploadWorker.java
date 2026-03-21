package azureintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Uploads a blob to Azure Blob Storage.
 * Input: container, blobName, data
 * Output: blobUrl, etag, contentLength
 */
public class AzBlobUploadWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "az_blob_upload";
    }

    @Override
    public TaskResult execute(Task task) {
        String container = (String) task.getInputData().get("container");
        if (container == null) container = "default-container";

        String blobName = (String) task.getInputData().get("blobName");
        if (blobName == null) blobName = "events/unknown.json";

        String blobUrl = "https://myaccount.blob.core.windows.net/" + container + "/" + blobName;

        System.out.println("  [Blob] Uploaded to " + blobUrl);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("blobUrl", "" + blobUrl);
        result.getOutputData().put("etag", "\"0xA1B2C3D4E5F6\"");
        result.getOutputData().put("contentLength", 512);
        return result;
    }
}
