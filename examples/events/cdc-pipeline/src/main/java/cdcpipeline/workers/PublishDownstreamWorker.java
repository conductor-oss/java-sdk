package cdcpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Publishes transformed CDC changes to a downstream topic.
 * Publishes events downstream and returns message IDs.
 */
public class PublishDownstreamWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cd_publish_downstream";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> transformedChanges =
                (List<Map<String, Object>>) task.getInputData().get("transformedChanges");
        if (transformedChanges == null) {
            transformedChanges = List.of();
        }

        String targetTopic = (String) task.getInputData().get("targetTopic");
        if (targetTopic == null || targetTopic.isBlank()) {
            targetTopic = "cdc.users.changes";
        }

        System.out.println("  [cd_publish_downstream] Publishing " + transformedChanges.size()
                + " messages to topic '" + targetTopic + "'");

        int count = transformedChanges.size();
        List<String> messageIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            messageIds.add("msg-fixed-" + i);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("publishResult", "success");
        result.getOutputData().put("messagesPublished", count);
        result.getOutputData().put("messageIds", messageIds);
        return result;
    }
}
