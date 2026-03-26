package prompttemplates.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Resolves a prompt template by substituting variables into placeholders.
 *
 * Template store contains versioned prompt templates keyed by "templateId:vVersion".
 * Each {{key}} placeholder in the template is replaced with the corresponding value
 * from the variables map.
 */
public class ResolveTemplateWorker implements Worker {

    private static final Map<String, PromptTemplate> TEMPLATE_STORE = Map.of(
            "summarize:v2", new PromptTemplate(
                    "summarize", 2,
                    "Summarize the following {{format}}:\n\nTopic: {{topic}}\nAudience: {{audience}}\n\nProvide a {{length}} summary."
            ),
            "classify:v1", new PromptTemplate(
                    "classify", 1,
                    "Classify the following text into one of these categories: {{categories}}.\n\nText: {{text}}\n\nCategory:"
            )
    );

    @Override
    public String getTaskDefName() {
        return "pt_resolve_template";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String templateId = (String) task.getInputData().get("templateId");
        Object versionObj = task.getInputData().get("templateVersion");
        int templateVersion = versionObj instanceof Number
                ? ((Number) versionObj).intValue()
                : Integer.parseInt(versionObj.toString());
        Map<String, String> variables = (Map<String, String>) task.getInputData().get("variables");

        String key = templateId + ":v" + templateVersion;

        PromptTemplate template = TEMPLATE_STORE.get(key);
        TaskResult result = new TaskResult(task);

        if (template == null) {
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Template not found: " + key);
            return result;
        }

        String resolved = template.template();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            resolved = resolved.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        System.out.println("  [pt_resolve_template] Resolved template " + key);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("resolvedPrompt", resolved);
        result.getOutputData().put("templateKey", key);
        return result;
    }

    /**
     * Returns the template store for testing purposes.
     */
    public static Map<String, PromptTemplate> getTemplateStore() {
        return TEMPLATE_STORE;
    }

    public record PromptTemplate(String id, int version, String template) {}
}
