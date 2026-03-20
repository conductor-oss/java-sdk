package s3integration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Generates a presigned URL for an S3 object.
 * Input: bucket, key, expiresIn
 * Output: presignedUrl, expiresAt
 */
public class GenerateUrlWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "s3_generate_url";
    }

    @Override
    public TaskResult execute(Task task) {
        String bucket = (String) task.getInputData().get("bucket");
        String key = (String) task.getInputData().get("key");
        Object expiresIn = task.getInputData().get("expiresIn");
        String presignedUrl = "https://" + bucket + ".s3.amazonaws.com/" + key + "?X-Amz-Expires=" + expiresIn + "&X-Amz-Signature=deterministic";
        System.out.println("  [url] Generated presigned URL (expires in " + expiresIn + "s)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("presignedUrl", "" + presignedUrl);
        result.getOutputData().put("expiresAt", "" + java.time.Instant.now().toString());
        return result;
    }
}
