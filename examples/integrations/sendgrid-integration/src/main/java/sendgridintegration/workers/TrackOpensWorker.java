package sendgridintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Tracks email opens.
 * Input: messageId, campaignId
 * Output: trackingEnabled, pixelInserted
 *
 * This worker is always deterministic.— open tracking setup is an internal
 * processing step that does not require an external API call.
 */
public class TrackOpensWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sgd_track_opens";
    }

    @Override
    public TaskResult execute(Task task) {
        String messageId = (String) task.getInputData().get("messageId");
        String campaignId = (String) task.getInputData().get("campaignId");
        System.out.println("  [track] Open tracking enabled for " + messageId + " in campaign " + campaignId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("trackingEnabled", true);
        result.getOutputData().put("pixelInserted", true);
        return result;
    }
}
