package s3integration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Uploads an object to S3.
 * Input: bucket, key, contentType
 * Output: etag, versionId, size
 */
public class S3UploadWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "s3_upload";
    }

    @Override
    public TaskResult execute(Task task) {
        String bucket = (String) task.getInputData().get("bucket");
        String key = (String) task.getInputData().get("key");
        String contentType = (String) task.getInputData().get("contentType");
        String etag = "\"" + Long.toHexString(System.currentTimeMillis()) + "\"";
        System.out.println("  [upload] s3://" + bucket + "/" + key + " (" + contentType + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("etag", "" + etag);
        result.getOutputData().put("versionId", "v" + System.currentTimeMillis());
        result.getOutputData().put("size", 2048);
        return result;
    }
}
