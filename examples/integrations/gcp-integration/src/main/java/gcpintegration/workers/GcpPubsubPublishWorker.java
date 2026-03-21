package gcpintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * perform  publishing a message to Google Cloud Pub/Sub.
 * Input: topic, message
 * Output: messageId, topic
 */
public class GcpPubsubPublishWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gcp_pubsub_publish";
    }

    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        if (topic == null) {
            topic = "projects/default/topics/default";
        }

        String messageId = "pubsub-msg-fixed-001";

        System.out.println("  [PubSub] Published " + messageId + " to " + topic);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("messageId", "" + messageId);
        result.getOutputData().put("topic", "" + topic);
        return result;
    }
}
