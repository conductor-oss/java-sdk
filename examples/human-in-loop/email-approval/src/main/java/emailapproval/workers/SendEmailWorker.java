package emailapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for ea_send_email — performs sending an approval email with click links.
 *
 * Generates approve and reject URLs based on the workflow instance ID,
 * then returns { sent: true, approveUrl: "...", rejectUrl: "..." }.
 */
public class SendEmailWorker implements Worker {

    private static final String BASE_URL = "https://example.com/approval";

    @Override
    public String getTaskDefName() {
        return "ea_send_email";
    }

    @Override
    public TaskResult execute(Task task) {
        String workflowId = task.getWorkflowInstanceId() != null
                ? task.getWorkflowInstanceId()
                : "unknown";

        String approveUrl = BASE_URL + "?workflowId=" + workflowId + "&action=approve";
        String rejectUrl = BASE_URL + "?workflowId=" + workflowId + "&action=reject";

        System.out.println("  [ea_send_email] Sending approval email for workflow " + workflowId);
        System.out.println("    Approve URL: " + approveUrl);
        System.out.println("    Reject URL:  " + rejectUrl);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sent", true);
        result.getOutputData().put("approveUrl", approveUrl);
        result.getOutputData().put("rejectUrl", rejectUrl);
        return result;
    }
}
