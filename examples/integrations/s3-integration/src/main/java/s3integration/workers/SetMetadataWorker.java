package s3integration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sets metadata on an S3 object.
 * Input: bucket, key, etag
 * Output: metadata, updated
 */
public class SetMetadataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "s3_set_metadata";
    }

    @Override
    public TaskResult execute(Task task) {
        String key = (String) task.getInputData().get("key");
        String etag = (String) task.getInputData().get("etag");

        java.util.Map<String, Object> metadata = java.util.Map.of(
                "uploadedBy", "workflow",
                "processedAt", "" + java.time.Instant.now().toString(),
                "etag", "" + String.valueOf(etag));
        System.out.println("  [metadata] Set metadata on " + key);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("metadata", metadata);
        result.getOutputData().put("updated", true);
        return result;
    }
}
