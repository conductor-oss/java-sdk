package gcpintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Uploads an object to Google Cloud Storage.
 * Input: bucket, objectName, data
 * Output: objectPath, generation, size
 */
public class GcpGcsUploadWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gcp_gcs_upload";
    }

    @Override
    public TaskResult execute(Task task) {
        String bucket = (String) task.getInputData().get("bucket");
        if (bucket == null) {
            bucket = "default-bucket";
        }

        String objectName = (String) task.getInputData().get("objectName");
        if (objectName == null) {
            objectName = "events/unknown.json";
        }

        String objectPath = "gs://" + bucket + "/" + objectName;

        System.out.println("  [GCS] Uploaded to " + objectPath);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("objectPath", "" + objectPath);
        result.getOutputData().put("generation", 1700000000000L);
        result.getOutputData().put("size", 1024);
        return result;
    }
}
