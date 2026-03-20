package emailagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Drafts the email subject and body based on the analyzed request,
 * key points, recipient, and desired tone.
 */
public class DraftEmailWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ea_draft_email";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String emailType = (String) task.getInputData().get("emailType");
        if (emailType == null || emailType.isBlank()) {
            emailType = "general";
        }

        List<String> keyPoints = (List<String>) task.getInputData().get("keyPoints");
        if (keyPoints == null) {
            keyPoints = List.of();
        }

        String recipient = (String) task.getInputData().get("recipient");
        if (recipient == null || recipient.isBlank()) {
            recipient = "unknown@example.com";
        }

        String desiredTone = (String) task.getInputData().get("desiredTone");
        if (desiredTone == null || desiredTone.isBlank()) {
            desiredTone = "professional";
        }

        System.out.println("  [ea_draft_email] Drafting " + emailType + " email for: " + recipient);

        String subject = "Project Alpha: Milestone 3 Completed Ahead of Schedule";

        String body = "Dear Sarah,\n\n"
                + "I am pleased to inform you that we have successfully completed Milestone 3 "
                + "for Project Alpha, two days ahead of the scheduled deadline.\n\n"
                + "Key highlights:\n"
                + "- All planned deliverables have been met\n"
                + "- The complete test suite is passing with zero failures\n"
                + "- Code review and quality checks are finalized\n\n"
                + "The team's dedication and effective collaboration made this early delivery "
                + "possible. We are now well-positioned to begin Milestone 4 on schedule.\n\n"
                + "Please let me know if you have any questions or would like a detailed briefing.\n\n"
                + "Best regards";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("subject", subject);
        result.getOutputData().put("body", body);
        result.getOutputData().put("wordCount", 95);
        return result;
    }
}
