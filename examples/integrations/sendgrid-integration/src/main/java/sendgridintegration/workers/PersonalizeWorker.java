package sendgridintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Personalizes an email for a recipient.
 * Input: template, recipientName, recipientEmail
 * Output: subject, htmlBody
 *
 * This worker is always deterministic.— personalization is an internal
 * processing step that does not require an external API call.
 */
public class PersonalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sgd_personalize";
    }

    @Override
    public TaskResult execute(Task task) {
        String recipientName = (String) task.getInputData().get("recipientName");
        String subject = "Welcome to our platform, " + recipientName + "!";
        String htmlBody = "<h1>Hello " + recipientName + "</h1><p>Welcome aboard!</p>";
        System.out.println("  [personalize] Personalized for " + recipientName);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("subject", subject);
        result.getOutputData().put("htmlBody", htmlBody);
        return result;
    }
}
