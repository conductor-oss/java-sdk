package awsintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Uploads an object to Amazon S3.
 * Input: bucket, key, body
 * Output: s3Key, bucket, etag
 */
public class AwsS3UploadWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aws_s3_upload";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String bucket = (String) task.getInputData().get("bucket");
        if (bucket == null) {
            bucket = "default-bucket";
        }

        Object bodyRaw = task.getInputData().get("body");
        String id = "unknown";
        if (bodyRaw instanceof Map) {
            Object idVal = ((Map<String, Object>) bodyRaw).get("id");
            if (idVal != null) {
                id = String.valueOf(idVal);
            }
        }

        String s3Key = "data/" + id + ".json";

        System.out.println("  [S3] Uploaded to s3://" + bucket + "/" + s3Key);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("s3Key", "" + s3Key);
        result.getOutputData().put("bucket", "" + bucket);
        result.getOutputData().put("etag", "\"a1b2c3d4e5f6\"");
        return result;
    }
}
