package s3integration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Notifies a user about a file upload.
 * Input: email, presignedUrl, key
 * Output: notified, sentAt
 */
public class S3NotifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "s3_notify";
    }

    @Override
    public TaskResult execute(Task task) {
        String email = (String) task.getInputData().get("email");
        String key = (String) task.getInputData().get("key");
        System.out.println("  [notify] Sent download link for " + key + " to " + email);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        result.getOutputData().put("sentAt", "" + java.time.Instant.now().toString());
        return result;
    }
}
