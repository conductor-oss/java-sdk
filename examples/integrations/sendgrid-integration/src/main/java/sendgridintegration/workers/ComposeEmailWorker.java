package sendgridintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Composes an email from a template.
 * Input: templateId, campaignId
 * Output: template
 *
 * This worker is always deterministic.— template composition is an internal
 * processing step that does not require an external API call.
 */
public class ComposeEmailWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sgd_compose_email";
    }

    @Override
    public TaskResult execute(Task task) {
        String templateId = (String) task.getInputData().get("templateId");
        String campaignId = (String) task.getInputData().get("campaignId");
        System.out.println("  [compose] Loading template " + templateId + " for campaign " + campaignId);

        java.util.Map<String, Object> template = java.util.Map.of(
                "id", templateId,
                "subject", "Welcome to our platform, {{name}}!",
                "body", "<h1>Hello {{name}}</h1><p>Welcome aboard!</p>");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("template", template);
        return result;
    }
}
