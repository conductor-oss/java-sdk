package slackapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker for sa_post_slack — generates a Slack Block Kit message payload
 * with approve/reject interactive buttons.
 *
 * In a real integration this would POST the payload to Slack's API.
 * Here we build the Block Kit JSON structure and return it as output
 * so the workflow can reference it.
 *
 * Returns { posted: true, slackPayload: { channel, blocks } }.
 */
public class PostSlackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sa_post_slack";
    }

    @Override
    public TaskResult execute(Task task) {
        String channel = "#approvals";
        Object channelInput = task.getInputData().get("channel");
        if (channelInput instanceof String && !((String) channelInput).isEmpty()) {
            channel = (String) channelInput;
        }

        String requestor = "unknown";
        Object requestorInput = task.getInputData().get("requestor");
        if (requestorInput instanceof String && !((String) requestorInput).isEmpty()) {
            requestor = (String) requestorInput;
        }

        String reason = "No reason provided";
        Object reasonInput = task.getInputData().get("reason");
        if (reasonInput instanceof String && !((String) reasonInput).isEmpty()) {
            reason = (String) reasonInput;
        }

        System.out.println("  [sa_post_slack] Generating Slack Block Kit payload for channel " + channel);

        // Build Slack Block Kit payload with approve/reject buttons
        Map<String, Object> headerBlock = Map.of(
                "type", "header",
                "text", Map.of(
                        "type", "plain_text",
                        "text", "Approval Request"
                )
        );

        Map<String, Object> sectionBlock = Map.of(
                "type", "section",
                "text", Map.of(
                        "type", "mrkdwn",
                        "text", "*Requestor:* " + requestor + "\n*Reason:* " + reason
                )
        );

        Map<String, Object> approveButton = Map.of(
                "type", "button",
                "text", Map.of(
                        "type", "plain_text",
                        "text", "Approve"
                ),
                "style", "primary",
                "action_id", "approve_action",
                "value", "approved"
        );

        Map<String, Object> rejectButton = Map.of(
                "type", "button",
                "text", Map.of(
                        "type", "plain_text",
                        "text", "Reject"
                ),
                "style", "danger",
                "action_id", "reject_action",
                "value", "rejected"
        );

        Map<String, Object> actionsBlock = Map.of(
                "type", "actions",
                "elements", List.of(approveButton, rejectButton)
        );

        List<Map<String, Object>> blocks = List.of(headerBlock, sectionBlock, actionsBlock);

        Map<String, Object> slackPayload = Map.of(
                "channel", channel,
                "blocks", blocks
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("posted", true);
        result.getOutputData().put("slackPayload", slackPayload);
        return result;
    }
}
